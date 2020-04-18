package app.flux.react.app.quiz

import app.flux.stores.quiz.GamepadStore.Arrow
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.Team
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
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

final class GamepadSetupView(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamInputStore: TeamInputStore,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamInputStore,
      _.copy(teamIdToGamepadState = teamInputStore.state.teamIdToGamepadState))
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        teams = teamsAndQuizStateStore.stateOrEmpty.teams,
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
      teamIdToGamepadState: Map[Long, GamepadState] = Map(),
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        pageHeader(router.currentPage),
        if (state.teams.isEmpty) {
          <.span("There are no teams yet, please add some and come back to this page then")
        } else {
          Bootstrap.Row(
            HalfPanel(title = <.span("Gamepads")) {
              <.span(
                Table(
                  tableClasses = Seq("table-gamepads"),
                  tableHeaders = Seq(
                    <.th("Team"),
                    <.th("Gamepad input"),
                  ),
                  tableRowDatas = tableRowDatas(state)
                ),
              )
            }
          )
        }
      )
    }

    private def tableRowDatas(implicit state: State): Seq[Table.TableRowData] = {
      for (team <- state.teams) yield {
        Table.TableRowData(
          Seq[VdomElement](
            <.td(
              ^.style := js.Dictionary("width" -> "600px"),
              team.name,
            ),
            <.td(
              ^.style := js.Dictionary("width" -> "200px"),
              <<.ifDefined(state.teamIdToGamepadState.get(team.id)) { gamepadState =>
                <<.ifThen(gamepadState.connected) {
                  <.span(
                    Bootstrap.FontAwesomeIcon("gamepad"),
                    gamepadState.arrowPressed match {
                      case None =>
                        Arrow.Up.icon(
                          ^.style := js.Dictionary("color" -> "white"),
                        )
                      case Some(arrow) => arrow.icon
                    },
                    Bootstrap.FontAwesomeIcon("circle")(
                      ^^.ifThen(!gamepadState.otherButtonPressed) {
                        ^.style := js.Dictionary("color" -> "white")
                      },
                    )
                  )
                }
              }
            )
          ))
      }
    }
  }
}
