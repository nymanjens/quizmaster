package app.flux.react.app.quiz

import app.api.ScalaJsApi.GetInitialDataResponse
import app.common.MasterSecretUtils
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import hydro.jsfacades.Recharts
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import scala.collection.immutable.Seq

final class SubmissionsSummaryView(implicit
    pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    i18n: I18n,
    getInitialDataResponse: GetInitialDataResponse,
    submissionsSummaryChart: SubmissionsSummaryChart,
    submissionsSummaryTable: SubmissionsSummaryTable,
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    MasterSecretUtils.requireMasterSecretOrRedirect(component(Props(router)), router)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        submissionsSummaryChart(selectedTeamId = None),
        submissionsSummaryTable(selectedTeamId = None),
      )
    }
  }
}
