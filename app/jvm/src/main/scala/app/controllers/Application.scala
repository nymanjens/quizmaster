package app.controllers

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
    val configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")
    val rootPath =
      Paths.get(ResourceFiles.canonicalizePath(configLocation)).getParent.resolve("images").toString
    externalAssetsController.at(rootPath = rootPath, file)
  }
}
