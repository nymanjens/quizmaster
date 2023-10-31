package hydro.controllers

import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

final class StandardActions @Inject() (implicit
    override val messagesApi: MessagesApi,
    components: ControllerComponents,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
    env: play.api.Environment,
) extends AbstractController(components)
    with I18nSupport {

  def index() = Action { implicit request =>
    Redirect(hydro.controllers.routes.StandardActions.reactAppRoot)
  }

  def reactAppRoot = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def reactApp(anyString: String) = reactAppRoot

  def reactAppWithoutCredentials = Action { implicit request =>
    Ok(views.html.reactApp())
  }

  def healthCheck = Action { implicit request =>
    Ok("OK")
  }
}
