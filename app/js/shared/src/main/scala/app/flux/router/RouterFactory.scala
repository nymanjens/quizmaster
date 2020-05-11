package app.flux.router

import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.models.access.EntityAccess
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.async.Async.async
import scala.async.Async.await
import scala.reflect.ClassTag
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[router] final class RouterFactory(
    implicit reactAppModule: app.flux.react.app.Module,
    dispatcher: Dispatcher,
    i18n: I18n,
    entityAccess: EntityAccess,
) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until(RouterFactory.pathPrefix), routerConfig)
  }

  private def routerConfig(implicit reactAppModule: app.flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        import dsl._
        val query: RouteB[String] = "?q=" ~ string(".+")

        def staticRuleFromPage(page: Page, renderer: RouterContext => VdomElement): dsl.Rule = {
          val path = RouterFactory.pathPrefix + page.getClass.getSimpleName.toLowerCase
          staticRoute(path, page) ~> renderR(ctl => logExceptions(renderer(RouterContext(page, ctl))))
        }
        def dynamicRuleFromPage[P <: Page](dynamicPart: String => RouteB[P])(
            renderer: (P, RouterContext) => VdomElement)(implicit pageClass: ClassTag[P]): dsl.Rule = {
          val staticPathPart = RouterFactory.pathPrefix + pageClass.runtimeClass.getSimpleName.toLowerCase
          val path = dynamicPart(staticPathPart)
          dynamicRouteCT(path) ~> dynRenderR {
            case (page, ctl) => logExceptions(renderer(page, RouterContext(page, ctl)))
          }
        }

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute(RouterFactory.pathPrefix, StandardPages.Root)
            ~> redirectToPage(AppPages.TeamSelection)(Redirect.Replace)

          | staticRuleFromPage(AppPages.TeamSelection, reactAppModule.teamController.forTeamSelection)

          | dynamicRuleFromPage(_ / long.caseClass[AppPages.TeamController]) { (page, ctl) =>
            reactAppModule.teamController(teamId = page.teamId, ctl)
          }

          | staticRuleFromPage(AppPages.Quiz, reactAppModule.quizView.apply)

          | staticRuleFromPage(AppPages.Master, reactAppModule.masterView.apply)

          | staticRuleFromPage(AppPages.QuizSettings, reactAppModule.quizSettingsView.apply)

          | staticRuleFromPage(AppPages.SubmissionsSummary, reactAppModule.submissionsSummaryView.apply)

          | staticRuleFromPage(AppPages.Gamepad, reactAppModule.gamepadSetupView.apply)

        // Fallback
        ).notFound(redirectToPage(StandardPages.Root)(Redirect.Replace))
          .onPostRender((_, currentPage) =>
            LogExceptionsCallback(dispatcher.dispatch(
              StandardActions.SetPageLoadingState(isLoading = false, currentPage = currentPage))))
          .onPostRender((_, page) =>
            LogExceptionsCallback(async {
              val title = await(page.title)
              dom.document.title = s"$title | Quizmaster"
            }))
      }
      .renderWith(layout)
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(
      implicit reactAppModule: app.flux.react.app.Module) = {
    reactAppModule.layout(RouterContext(resolution.page, routerCtl))(
      <.div(^.key := resolution.page.toString, resolution.render()))
  }
}
private[router] object RouterFactory {
  val pathPrefix = "/app/"
}
