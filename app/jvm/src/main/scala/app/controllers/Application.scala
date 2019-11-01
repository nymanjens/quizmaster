package app.controllers

import org.reactivestreams.Subscriber
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import akka.stream.scaladsl.StreamConverters
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import app.api.ScalaJsApiServerFactory
import app.common.QuizAssets
import app.models.access.JvmEntityAccess
import app.models.quiz.QuizState
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
}
