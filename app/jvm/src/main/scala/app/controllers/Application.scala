package app.controllers

import app.api.ScalaJsApiServerFactory
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.time.Clock
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
) extends AbstractController(components)
    with I18nSupport {}
