package app.flux.react.app.quiz

import app.api.ScalaJsApi.GetInitialDataResponse
import app.flux.stores.quiz.GamepadStore.Arrow
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.Team
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Table
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.collection.immutable.Seq
import scala.scalajs.js

final class QuizSettingsView(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamInputStore: TeamInputStore,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    i18n: I18n,
    getInitialDataResponse: GetInitialDataResponse,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(masterSecret: String, router: RouterContext): VdomElement = {
    require(getInitialDataResponse.masterSecret == masterSecret)
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        teams = teamsAndQuizStateStore.stateOrEmpty.teams,
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        "Hello World!",
      )
    }
  }
}
