package app.flux.react.app

import app.common.FixedPointNumber
import app.flux.react.app.quiz.TeamsList
import app.flux.router.AppPages
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.flux.ClientApp.HtmlImage
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.SbadminLayout
import hydro.flux.react.HydroReactComponent
import hydro.flux.router.RouterContext
import hydro.jsfacades.Audio
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.scalajs.js

final class Layout(implicit
    sbadminLayout: SbadminLayout,
    teamsList: TeamsList,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizConfig: QuizConfig,
) extends HydroReactComponent {

  // Keep a reference to preloaded media to avoid garbage collection cleaning it up
  private val preloadedImages: mutable.Buffer[HtmlImage] = mutable.Buffer()
  private val preloadedAudios: mutable.Buffer[Audio] = mutable.Buffer()

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router, children.toVector))
  }

  // **************** Implementation of HydroReactComponent methods **************** //
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class State(
      boundShortcutsAndPreloadedMedia: Boolean = false
  )
  protected case class Props(router: RouterContext, children: Seq[VdomNode])

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = {
      implicit val router = props.router

      // This would normally be done via DidMount and DidUpdate hooks, but due to a bug in React or japgolly's
      // React wrapper, this isn't possible because duplicate hooks in the same page are mixed together
      // (leading to ClassCastExceptions).
      maybeScheduleBindShortcutsAndPreloadMedia(state)

      sbadminLayout(
        title = "Quizmaster",
        leftMenu = <.span(),
        pageContent = <.div(
          ^.id := "content-wrapper",
          <<.ifThen(AppPages.isMasterOnlyPage(router.currentPage)) {
            <.div(
              ^.id := "left-content-wrapper",
              teamsList(showMasterControls = router.currentPage == AppPages.Master),
            )
          },
          <.div(
            ^.id := "right-content-wrapper",
            props.children.toVdomArray,
          ),
        ),
      )
    }

    private def maybeScheduleBindShortcutsAndPreloadMedia(
        state: State
    )(implicit router: RouterContext): Unit = {
      if (!state.boundShortcutsAndPreloadedMedia && AppPages.isMasterOnlyPage(router.currentPage)) {
        js.timers.setTimeout(300.milliseconds) {
          val updatedState = $.state.runNow()
          if (!updatedState.boundShortcutsAndPreloadedMedia) {
            $.modState(_.copy(boundShortcutsAndPreloadedMedia = true)).runNow()
            bindShortcuts()
            preloadMedia()
          }
        }
      }
    }

    private def bindShortcuts(): Unit = {
      println("  Binding shortcuts...")

      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(
          shortcut,
          e => {
            e.preventDefault()
            runnable()
          },
        )
      }
      def bindGlobal(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bindGlobal(
          shortcut,
          e => {
            e.preventDefault()
            runnable()
          },
        )
      }

      // Quiz navigation
      bind("left", () => teamsAndQuizStateStore.goToPreviousStep())
      bind("right", () => teamsAndQuizStateStore.goToNextStep())
      bind("alt+left", () => teamsAndQuizStateStore.goToPreviousQuestion())
      bind("alt+right", () => teamsAndQuizStateStore.goToNextQuestion())
      bind("alt+shift+left", () => teamsAndQuizStateStore.goToPreviousRound())
      bind("alt+shift+right", () => teamsAndQuizStateStore.goToNextRound())
      bind("alt+shift+r", () => teamsAndQuizStateStore.resetCurrentQuestion())
      bind("alt+enter", () => teamsAndQuizStateStore.toggleImageIsEnlarged())

      // Give points
      for ((teamIndex, shortkey) <- (0 to 10) zip ((1 to 9) :+ 0)) {
        bind(
          s"$shortkey",
          () => teamsAndQuizStateStore.updateScore(teamIndex, scoreDiff = FixedPointNumber(+1)),
        )
        bind(
          s"shift+$shortkey",
          () => teamsAndQuizStateStore.updateScore(teamIndex, scoreDiff = FixedPointNumber(-1)),
        )
      }
    }

    private def preloadMedia(): Unit = {
      println("  Preloading media...")

      for {
        round <- quizConfig.rounds
        question <- round.questions
      } {
        question match {
          case single: Question.Standard =>
            for (image <- Seq() ++ single.image ++ single.answerImage) {
              val htmlImage = new HtmlImage()
              htmlImage.asInstanceOf[js.Dynamic].src = s"/quizassets/${image.src}"
              preloadedImages.append(htmlImage)
            }

            for (audioSrc <- single.audioSrc) {
              val audio = new Audio(s"/quizassets/$audioSrc")
              preloadedAudios.append(audio)
            }

          // Don't preload video because it's too large

          case _ =>
        }
      }
    }
  }
}
