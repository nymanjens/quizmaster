package hydro.flux.react.uielements

import app.models.user.User
import app.AppVersion
import app.api.ScalaJsApi.GetInitialDataResponse
import app.common.LocalStorageClient
import app.flux.router.AppPages
import app.models.quiz.config.QuizConfig
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.models.access.JsEntityAccess
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js

final class SbadminLayout(implicit
    globalMessages: GlobalMessages,
    pageLoadingSpinner: PageLoadingSpinner,
    applicationDisconnectedIcon: ApplicationDisconnectedIcon,
    pendingModificationsCounter: PendingModificationsCounter,
    user: User,
    i18n: I18n,
    quizConfig: QuizConfig,
    jsEntityAccess: JsEntityAccess,
    dispatcher: Dispatcher,
    getInitialDataResponse: GetInitialDataResponse,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(title: TagMod, leftMenu: VdomElement, pageContent: VdomElement)(implicit
      router: RouterContext
  ): VdomElement = {
    component(Props(title, leftMenu, pageContent, router))
  }

  // **************** Implementation of HydroReactComponent methods **************** //
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class State(
      isQuizMaster: Boolean = {
        LocalStorageClient.getMasterSecret() == Some(quizConfig.masterSecret)
      }
  )
  protected case class Props(
      title: TagMod,
      leftMenu: VdomElement,
      pageContent: VdomElement,
      router: RouterContext,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router
      <.div(
        ^.id := "wrapper",
        // Navigation
        <.nav(
          ^.className := "navbar navbar-default navbar-static-top",
          ^.style := js.Dictionary("marginBottom" -> 0),
          <.div(
            ^.className := "navbar-header",
            Bootstrap.NavbarBrand(tag = router.anchorWithHrefTo(StandardPages.Root))(props.title),
            " ",
            pageLoadingSpinner(),
          ),
          if (state.isQuizMaster) {
            <.ul(
              ^.className := "nav navbar-top-links navbar-right",
              applicationDisconnectedIcon(),
              pendingModificationsCounter(),
              versionNavbar(),
              <.li(linkToPage(AppPages.TeamSelection, "Player input")),
              <.li(linkToPage(AppPages.Quiz, "Quiz view")),
              <.li(linkToPage(AppPages.Master, "Master view")),
              <.li(
                <.a(
                  ^.href := s"/rounds/${quizConfig.masterSecret}/",
                  Bootstrap.FontAwesomeIcon("bar-chart-o", fixedWidth = true),
                  tooltip("Question stats"),
                )
              ),
              <.li(linkToPage(AppPages.SubmissionsSummary, "Answers")),
              <.li(linkToPage(AppPages.Gamepad, "Gamepads")),
              <.li(linkToPage(AppPages.QuizSettings, "Settings")),
              <.li(
                <.a(
                  ^.href := "javascript:void(0)",
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    e.preventDefault()
                    LocalStorageClient.removeMasterSecret()
                    $.modState(_.copy(isQuizMaster = false)).thenRun {
                      router.setPage(AppPages.TeamSelection)
                    }
                  },
                  Bootstrap.FontAwesomeIcon("lock", fixedWidth = true),
                  tooltip("Logout", alignRight = true),
                )
              ),
            )
          } else {
            <.ul(
              ^.className := "nav navbar-top-links navbar-right",
              applicationDisconnectedIcon(),
              pendingModificationsCounter(),
              versionNavbar(),
              <.li(
                <.a(
                  ^.id := "master-secret-prompt-link",
                  ^.href := "javascript:void(0)",
                  ^.onClick ==> { (e: ReactEventFromInput) =>
                    e.preventDefault()
                    promptMasterSecret() match {
                      case None => Callback.empty
                      case Some(masterSecret) =>
                        LocalStorageClient.setMasterSecret(masterSecret)
                        $.modState(_.copy(isQuizMaster = true)).thenRun {
                          router.setPage(AppPages.Quiz)
                        }
                    }
                  },
                  Bootstrap.FontAwesomeIcon("unlock", fixedWidth = true),
                  tooltip("Login as master", alignRight = true),
                )
              ),
            )
          },
        ),
        // Page Content
        <.div(
          ^.id := "page-wrapper",
          <.div(
            ^.className := "container-fluid",
            Bootstrap.Row(
              Bootstrap.Col(lg = 12)(
                ^.style := js.Dictionary(
                  "padding" -> "0px"
                ),
                globalMessages(),
                props.pageContent,
              )
            ),
          ),
        ),
      )
    }

    // **************** Private helper methods ****************//
    private def versionNavbar(): VdomNode = {
      Bootstrap.NavbarBrand(tag = <.a)(
        ^.href := s"/versions/",
        ^.style := js.Dictionary(
          "marginRight" -> "15px"
        ),
        s"v${AppVersion.versionString}",
      )
    }

    private def linkToPage(page: Page, tooltipText: String)(implicit router: RouterContext): VdomElement = {
      router.anchorWithHrefTo(page)(
        ^^.ifThen(router.currentPage == page) {
          ^.className := "selected"
        },
        <.i(
          ^.className := page.iconClass
        ),
        tooltip(tooltipText),
      )
    }

    private def tooltip(tooltipText: String, alignRight: Boolean = false): TagMod = {
      TagMod(
        ^.className := "with-tooltip",
        <.span(
          ^.className := "tooltiptext",
          ^^.ifThen(alignRight)(^.className := "tooltiptext-align-right"),
          tooltipText,
        ),
      )
    }

    private def promptMasterSecret(): Option[String] = {
      if (quizConfig.masterSecret == "*") {
        Some(quizConfig.masterSecret)
      } else {
        val userInput = dom.window.prompt(i18n("app.enter-master-secret"))
        if (userInput == null) {
          // Canceled
          None
        } else if (userInput != quizConfig.masterSecret) {
          dom.window.alert("Wrong secret")
          None
        } else {
          Some(userInput)
        }
      }
    }

    private def navbarCollapsed: Boolean = {
      // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
      val width = if (dom.window.innerWidth > 0) dom.window.innerWidth else dom.window.screen.width
      width < 768
    }
  }
}
