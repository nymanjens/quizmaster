package app.flux.react.app.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class ObfuscatedAnswer(implicit
    clock: Clock,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(answer: String): VdomElement = {
    component(Props(answer = answer))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(answer: String)
  protected case class State(obfuscated: Boolean = true)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) with DidMount {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      <.div(
        ^.className := "maybe-obfuscated-answer-wrapper",
        "Answer: ",
        <.span(
          ^.className := "maybe-obfuscated-answer",
          ^^.ifThen(state.obfuscated) {
            ^.className := "obfuscated"
          },
          <<.nl2BrBlockWithLinks(props.answer),
        ),
        "(Toggle with ",
        <.kbd("A"),
        " )",
      )
    }

    override def didMount(props: Props, state: State): Callback = {
      bindShortcuts()
      Callback.empty
    }

    private def bindShortcuts(): Unit = {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(
          shortcut,
          e => {
            e.preventDefault()
            runnable()
          },
        )
      }
      bind("a", () => toggleObfuscated())
    }

    private def toggleObfuscated(): Unit = {
      $.modState(state => {
        state.copy(obfuscated = !state.obfuscated)
      }).runNow()
    }
  }
}
