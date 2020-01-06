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
    require(secret == playConfiguration.get[String]("app.quiz.secret"))

    def round1(double: Double): String = "%,.1f".format(double)
    def indent(width: Int, any: Any): String = s"%${width}s".format(any)

    def questionsInfo(prefix: String, questions: Iterable[Question]): String = {
      val minutes = {
        val maybeZeroMinutes = questions
          .map(_ match {
            case q: Question.Single => if (q.maxTime > Duration.ofMinutes(900)) Duration.ZERO else q.maxTime
            case _: Question.Double => Duration.ofMinutes(1)
          })
          .sum
          .toMinutes
          .toDouble
        if (maybeZeroMinutes == 0) 1 else maybeZeroMinutes
      }

      val maxPoints = questions.map(_.pointsToGainOnFirstAnswer).sum
      val avgPoints4PerfectTeams = questions.map { q =>
        if (q.onlyFirstGainsPoints) {
          q.pointsToGainOnFirstAnswer / 4.0
        } else {
          (q.pointsToGainOnFirstAnswer + 3 * q.pointsToGain) / 4.0
        }
      }.sum

      s"${indent(30, prefix)}: " +
        s"${indent(3, questions.size)} questions; ${indent(3, minutes.round)} min;    " +
        s"points: {max: ${indent(2, maxPoints)} (${indent(3, round1(maxPoints / minutes))} per min), " +
        s"avg4PerfectTeams: ${indent(4, round1(avgPoints4PerfectTeams))} (${indent(3, round1(avgPoints4PerfectTeams / minutes))} per min)}\n"
    }

    var result = ""
    result += "Rounds info:\n"
    result += "\n"

    for (round <- quizConfig.rounds) {
      result += questionsInfo(s"${round.name}", round.questions)
    }

    result += "\n"
    result += questionsInfo("Total", quizConfig.rounds.flatMap(_.questions))
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
        result += s"    - toGain: ${indent(2, q.pointsToGain)};  " +
          s"first: ${indent(2, q.pointsToGainOnFirstAnswer)};   " +
          s"onlyFirst: ${indent(5, q.onlyFirstGainsPoints)};" +
          s"${indent(6, round1(q.maxTime.getSeconds / 60.0))} min;" +
          s"${indent(100, textualQuestion)};" +
          s"${indent(40, textualAnswer)}\n"
      }
    }
    result += "\n"
    Ok(result)
  }
}
