package hydro.flux.react.uielements

import app.models.user.User
import app.AppVersion
import app.api.ScalaJsApi.GetInitialDataResponse
import app.flux.router.AppPages
import hydro.common.CollectionUtils.ifThenSeq
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.models.access.JsEntityAccess
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class SbadminLayout(
    implicit globalMessages: GlobalMessages,
    pageLoadingSpinner: PageLoadingSpinner,
    applicationDisconnectedIcon: ApplicationDisconnectedIcon,
    pendingModificationsCounter: PendingModificationsCounter,
    user: User,
    i18n: I18n,
    jsEntityAccess: JsEntityAccess,
    dispatcher: Dispatcher,
    getInitialDataResponse: GetInitialDataResponse,
) {

  // **************** API ****************//
  def apply(title: TagMod, leftMenu: VdomElement, pageContent: VdomElement)(
      implicit router: RouterContext): VdomElement = {
    <.div(
      ^.id := "wrapper",
      // Navigation
      <.nav(
        ^.className := "navbar navbar-default navbar-static-top",
        ^.style := js.Dictionary("marginBottom" -> 0),
        <.div(
          ^.className := "navbar-header",
          <.button(
            ^.className := "navbar-toggle",
            VdomAttr("data-toggle") := "collapse",
            VdomAttr("data-target") := ".navbar-collapse",
            <.span(^.className := "sr-only", "Toggle navigation"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar")
          ),
          Bootstrap.NavbarBrand(tag = router.anchorWithHrefTo(StandardPages.Root))(title),
          " ",
          pageLoadingSpinner()
        ),
        <.ul(
          ^.className := "nav navbar-top-links navbar-right",
          applicationDisconnectedIcon(),
          pendingModificationsCounter(),
          Bootstrap.NavbarBrand()(
            ^.style := js.Dictionary(
              "marginRight" -> "15px",
            ),
            s"v${AppVersion.versionString}",
          ),
          <.li(
            router.anchorWithHrefTo(AppPages.Quiz)(
              Bootstrap.FontAwesomeIcon("question-circle", fixedWidth = true),
            ),
          ),
          <.li(
            <.a(
              ^.onClick --> LogExceptionsCallback {
                promptMasterSecret match {
                  case None               =>
                  case Some(masterSecret) => router.setPage(AppPages.Master(masterSecret))
                }
              },
              Bootstrap.FontAwesomeIcon("gears", fixedWidth = true),
            ),
          ),
          <.li(
            <.a(
              ^.onClick --> LogExceptionsCallback {
                promptMasterSecret match {
                  case None               =>
                  case Some(masterSecret) => dom.window.location.href = s"/rounds/$masterSecret/"
                }
              },
              Bootstrap.FontAwesomeIcon("info-circle", fixedWidth = true),
            ),
          ),
          <.li(
            router.anchorWithHrefTo(AppPages.Gamepad)(
              Bootstrap.FontAwesomeIcon("gamepad", fixedWidth = true),
            ),
          ),
        ),
      ),
      // Page Content
      <.div(
        ^.id := "page-wrapper",
        <.div(
          ^.className := "container-fluid",
          Bootstrap.Row(
            Bootstrap.Col(lg = 12)(
              ^.style := js.Dictionary(
                "padding" -> "0px",
              ),
              globalMessages(),
              pageContent,
            )
          )
        )
      )
    )
  }

  // **************** Private helper methods ****************//
  private def promptMasterSecret(): Option[String] = {
    val userInput = dom.window.prompt(i18n("app.enter-master-secret"))
    if (userInput == null) {
      // Canceled
      None
    } else if (userInput != getInitialDataResponse.masterSecret) {
      dom.window.alert("Wrong password")
      None
    } else {
      Some(userInput)
    }
  }

  private def navbarCollapsed: Boolean = {
    // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val width = if (dom.window.innerWidth > 0) dom.window.innerWidth else dom.window.screen.width
    width < 768
  }
}
