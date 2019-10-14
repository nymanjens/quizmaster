package app.controllers

import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.time.Clock
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

final class ExternalApi @Inject()(
    implicit override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
) extends AbstractController(components)
    with I18nSupport {}
