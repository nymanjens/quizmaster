package app.flux.react.app.quiz

import app.api.ScalaJsApi.GetInitialDataResponse
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class GeneralQuizSettings(
    implicit i18n: I18n,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()
  protected case class State(
      quizState: QuizState = QuizState.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      <.span(
        Bootstrap.Row(
          HalfPanel(
            title = <.span("General quiz settings"),
          ) {
            "Test"
          }
        )
      )
    }
  }
}
