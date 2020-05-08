package app.controllers

import hydro.common.time.JavaTimeImplicits._
import org.reactivestreams.Subscriber
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.LocalDateTime

import akka.stream.scaladsl.StreamConverters
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import app.api.ScalaJsApiServerFactory
import app.common.QuizAssets
import app.models.access.JvmEntityAccess
import app.models.quiz.QuizState
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.common.ResourceFiles
import hydro.models.modification.EntityModification
import javax.inject.Singleton
import org.reactivestreams.Subscription
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
final class Application @Inject()(
    implicit override val messagesApi: MessagesApi,
    playConfiguration: play.api.Configuration,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    scalaJsApiServerFactory: ScalaJsApiServerFactory,
    env: play.api.Environment,
    executionContext: ExecutionContext,
    externalAssetsController: controllers.ExternalAssets,
    quizAssets: QuizAssets,
    quizConfig: QuizConfig,
) extends AbstractController(components)
    with I18nSupport {

  // Print round timings
  entityAccess.entityModificationPublisher.subscribe(new Subscriber[EntityModificationsWithToken]() {
    var lastSeenRoundIndex = -1

    override def onSubscribe(s: Subscription): Unit = {}
    override def onNext(t: EntityModificationsWithToken): Unit = {
      t.modifications.collect {
        case EntityModification.Add(quizState: QuizState) =>
          println(s"  >>>> [$currentTimeString] Started round ${quizState.roundIndex + 1}")
          lastSeenRoundIndex = quizState.roundIndex
          lastSeenRoundIndex = quizState.roundIndex
        case EntityModification.Update(quizState: QuizState) =>
          if (quizState.roundIndex != lastSeenRoundIndex) {
            println(
              s"  >>>> [$currentTimeString] Changed round from ${lastSeenRoundIndex + 1} to ${quizState.roundIndex + 1}")
            lastSeenRoundIndex = quizState.roundIndex
          }
      }
    }
    override def onError(t: Throwable): Unit = {}
    override def onComplete(): Unit = {}

    private def currentTimeString: String =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now)
  })

  def quizImage(file: String): Action[AnyContent] = {
    serveAsset(quizAssets.quizImage(file))
  }

  def quizAudio(file: String): Action[AnyContent] = {
    serveAsset(quizAssets.quizAudio(file))
  }

  def serveAsset(assetPath: Path): Action[AnyContent] = Action { implicit request =>
    val connection = assetPath.toFile.toURI.toURL.openConnection()
    val stream = connection.getInputStream
    val source = StreamConverters.fromInputStream(() => stream)
    RangeResult
      .ofSource(
        entityLength = stream.available(), // TODO: This may not be entirely accurate
        source = source,
        rangeHeader = request.headers.get(RANGE),
        fileName = None,
        contentType = None // TODO: Set content type
      )
  }

  def roundsInfo(secret: String) = Action { implicit request =>
    require(secret == quizConfig.masterSecret)

    val infiniteDurationThreshold = Duration.ofMinutes(900)
    def round1(double: Double): String = "%,.1f".format(double)
    def indent(width: Int, any: Any): String = {
      val string = any.toString
      if (width > 5 && string.length > width) { // If string is too long
        string.substring(0, width - 2) + ".."
      } else {
        s"%${width}s".format(string)
      }
    }

    def questionsInfo(questions: Iterable[Question], expectedTime: Option[Duration]): String = {
      val maxMinutes = {
        val maybeZeroMinutes = questions
          .map(_ match {
            case q: Question.Single =>
              if (q.maxTime > infiniteDurationThreshold) Duration.ofSeconds(30) else q.maxTime
            case _: Question.Double => Duration.ofSeconds(20)
          })
          .sum
          .toMinutes
          .toDouble
        if (maybeZeroMinutes == 0) 1 else maybeZeroMinutes
      }
      val expectedMinutes = expectedTime.map(_.toMinutes.toDouble) getOrElse maxMinutes

      val maxPoints = questions.map(_.pointsToGainOnFirstAnswer).sum
      val avgPoints4PerfectTeams = questions.map { q =>
        if (q.onlyFirstGainsPoints) {
          q.pointsToGainOnFirstAnswer / 4.0
        } else {
          (q.pointsToGainOnFirstAnswer + 3 * q.pointsToGain) / 4.0
        }
      }.sum
      val showMax = avgPoints4PerfectTeams.round != maxPoints
      val maxString =
        s", max: ${indent(3, maxPoints)} (${indent(3, round1(maxPoints / expectedMinutes))} per min)"

      s"${indent(3, questions.size)} questions; " +
        s"Time: {expected: ${indent(3, expectedMinutes.round)} min, max ${indent(3, maxMinutes.round)} min};    " +
        s"points: {avg4PerfectTeams: ${indent(5, round1(avgPoints4PerfectTeams))} (${indent(3, round1(avgPoints4PerfectTeams / expectedMinutes))} per min)" +
        (if (showMax) maxString else "") +
        "}"
    }

    def sumExpectedTimeOrNone(rounds: Seq[QuizConfig.Round]): Option[Duration] = {
      val expectedTimes = rounds.map(_.expectedTime)

      if (expectedTimes contains None) {
        None
      } else {
        Some(expectedTimes.map(_.get).sum)
      }
    }

    var result = ""
    result += "Rounds info:\n"
    result += "\n"

    for (round <- quizConfig.rounds) {
      result += s"${indent(30, round.name)}: ${questionsInfo(round.questions, round.expectedTime)}\n"
    }

    result += "\n"
    result += s"${indent(30, "Total")}: ${questionsInfo(quizConfig.rounds.flatMap(_.questions), expectedTime = sumExpectedTimeOrNone(quizConfig.rounds))}\n"
    result += "\n"
    result += "\n"
    result += "Details\n"
    result += "\n"
    for (round <- quizConfig.rounds) {
      result += s"- ${round.name}\n"

      for (q <- round.questions) {
        val (textualQuestion, textualAnswer) =
          q match {
            case question: Question.Single => (question.question, question.answer)
            case question: Question.Double => (question.textualQuestion, question.textualAnswer)
          }
        val maxTime =
          if (q.maxTime > infiniteDurationThreshold) "inf" else round1(q.maxTime.getSeconds / 60.0)

        result += s"    - toGain: ${indent(2, q.pointsToGain)};  " +
          s"first: ${indent(2, q.pointsToGainOnFirstAnswer)};   " +
          s"onlyFirst: ${indent(5, q.onlyFirstGainsPoints)}; " +
          s"${indent(5, maxTime)} min; " +
          s"${indent(50, textualQuestion)}; " +
          s"${indent(40, textualAnswer)}\n"
      }

      result += s"     ${questionsInfo(round.questions, round.expectedTime)}\n"
      result += "\n"
    }
    result += "\n"
    Ok(result)
  }
}
