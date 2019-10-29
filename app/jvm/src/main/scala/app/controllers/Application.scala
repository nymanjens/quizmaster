package app.controllers

import java.nio.file.Path
import java.nio.file.Paths

import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.common.ResourceFiles
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.ExecutionContext

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
) extends AbstractController(components)
    with I18nSupport {

  def quizImage(file: String): Action[AnyContent] = {
    externalAssetsController.at(rootPath = configPath.resolve("images").toString, file)
  }

  def quizAudio(file: String): Action[AnyContent] = {
    externalAssetsController.at(rootPath = configPath.resolve("audio").toString, file)
  }

  lazy val configPath: Path = {
    val configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")
    Paths.get(ResourceFiles.canonicalizePath(configLocation)).getParent
  }
}
