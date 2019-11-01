package app.flux.react.app

import app.flux.react.app.quiz.TeamsList
import app.flux.router.AppPages
import app.flux.stores.quiz.TeamsAndQuizStateStore
import hydro.flux.react.uielements.SbadminLayout
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.Callback

final class Layout(
    implicit sbadminLayout: SbadminLayout,
    teamsList: TeamsList,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router, children.toVector))
  }

  // **************** Implementation of HydroReactComponent methods **************** //
  override protected val statelessConfig =
    StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext, children: Seq[VdomNode])

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) with DidMount {

    override def render(props: Props, state: Unit): VdomElement = {
      implicit val router = props.router
      sbadminLayout(
        title = "Quizmaster",
        leftMenu = <.span(),
        pageContent = <.div(
          ^.id := "content-wrapper",
          <.div(
            ^.id := "left-content-wrapper",
            teamsList(showScoreEditButtons = router.currentPage == AppPages.Master),
          ),
          <.div(
            ^.id := "right-content-wrapper",
            props.children.toVdomArray,
          ),
        ),
      )
    }

    override def didMount(props: Props, state: Unit): Callback = {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      def bindGlobal(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bindGlobal(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }

      // Quiz navigation
      bind("left", () => teamsAndQuizStateStore.goToPreviousStep())
      bind("right", () => teamsAndQuizStateStore.goToNextStep())
      bind("ctrl+left", () => teamsAndQuizStateStore.goToPreviousQuestion())
      bind("ctrl+right", () => teamsAndQuizStateStore.goToNextQuestion())
      bind("ctrl+shift+left", () => teamsAndQuizStateStore.goToPreviousRound())
      bind("ctrl+shift+right", () => teamsAndQuizStateStore.goToNextRound())
      bind("alt+shift+r", () => teamsAndQuizStateStore.resetCurrentQuestion())

      // Give points
      for (teamIndex <- 0 to 4) {
        bind(s"${teamIndex + 1}", () => teamsAndQuizStateStore.updateScore(teamIndex, scoreDiff = +1))
        bind(s"shift+${teamIndex + 1}", () => teamsAndQuizStateStore.updateScore(teamIndex, scoreDiff = -1))
      }

      Callback.empty
    }
  }
}
