package app.flux.react.app.quiz

import hydro.flux.react.ReactVdomUtils.<<
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class TeamsList(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(showScoreEditButtons: Boolean): VdomElement = {
    component(Props(showScoreEditButtons = showScoreEditButtons))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(
          teams = teamsAndQuizStateStore.stateOrEmpty.teams,
        ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(showScoreEditButtons: Boolean)
  protected case class State(
      teams: Seq[Team] = Seq(),
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      <.ul(
        ^.className := "teams-list",
        (for (team <- state.teams) yield {
          <.li(
            ^.key := team.id,
            <.div(
              ^.className := "name",
              team.name
            ),
            <.div(
              ^.className := "score",
              <<.ifThen(props.showScoreEditButtons) {
                Bootstrap
                  .Button()(
                    ^.onClick --> LogExceptionsCallback(
                      teamsAndQuizStateStore.updateScore(team, scoreDiff = -1)).void,
                    Bootstrap.Glyphicon("minus"),
                  )
              },
              " ",
              team.score,
              " ",
              <<.ifThen(props.showScoreEditButtons) {
                Bootstrap
                  .Button()(
                    ^.onClick --> LogExceptionsCallback(
                      teamsAndQuizStateStore.updateScore(team, scoreDiff = +1)).void,
                    Bootstrap.Glyphicon("plus"),
                  )
              },
            ),
          )
        }).toVdomArray
      )
    }
  }
}
