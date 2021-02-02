package app.flux.react.app.quiz

import java.time.Duration

import app.api.ScalaJsApi.TeamOrQuizStateUpdate._
import app.api.ScalaJsApiClient
import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.QuizState.TimerState
import app.models.quiz.config.QuizConfig
import hydro.common.time.JavaTimeImplicits._
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.ReactVdomUtils.<<
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

final class SyncedTimerBar(implicit
    clock: Clock,
    soundEffectController: SoundEffectController,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    scalaJsApiClient: ScalaJsApiClient,
    quizConfig: QuizConfig,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(
          timerState = teamsAndQuizStateStore.stateOrEmpty.quizState.timerState,
          maxTime = teamsAndQuizStateStore.stateOrEmpty.quizState.maybeQuestion match {
            case Some(q) => q.maxTime
            case None    => Duration.ZERO
          },
          timerIsEnabled = teamsAndQuizStateStore.stateOrEmpty.quizState.maybeQuestion match {
            case Some(q) =>
              q.shouldShowTimer(teamsAndQuizStateStore.stateOrEmpty.quizState.questionProgressIndex)
            case None => false
          },
        ),
      )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()
  protected case class State(
      timerState: TimerState = TimerState.nullInstance,
      elapsedTime: Duration = Duration.ZERO,
      maxTime: Duration = Duration.ZERO,
      timerIsEnabled: Boolean = false,
  )

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with WillUnmount {

    private var intervalHandle: Option[Int] = None

    override def render(props: Props, state: State): VdomElement = logExceptions {
      if (state.timerIsEnabled) {
        val timeRemaining = Seq(state.maxTime - state.elapsedTime, Duration.ZERO).max
        val timeRemainingFraction = timeRemaining / state.maxTime

        <.div(
          ^.className := "synced-timer-bar",
          Bootstrap.ProgressBar(
            fraction = timeRemainingFraction,
            variant = {
              if (state.timerState.timerRunning) {
                if (timeRemainingFraction < 0.1) Variant.danger else Variant.default
              } else {
                Variant.success
              }
            },
            striped = !state.timerState.timerRunning,
            label = <.div(
              ^.className := "synced-timer-bar-label",
              s"${formatDuration(timeRemaining)} / ${formatDuration(state.maxTime)}",
            ),
          ),
          <<.ifThen(
            timeRemaining > Duration.ZERO &&
              timeRemaining < soundEffectController.timerAlmostRunningOutDetails.duration &&
              soundEffectController.timerAlmostRunningOutDetails.canPlayOnCurrentPage
          ) {
            RawMusicPlayer(
              src = soundEffectController.timerAlmostRunningOutDetails.filepath,
              playing = state.timerState.timerRunning,
            )
          },
        )
      } else {
        <.div(
          ^.className := "synced-timer-bar",
          Bootstrap.ProgressBar(
            fraction = 0,
            variant = Variant.default,
          ),
        )
      }
    }

    override def didMount(props: Props, state: State): Callback = {
      intervalHandle = Some(
        dom.window.setInterval(
          () => {
            $.modState { state =>
              if (state.timerIsEnabled) {
                val newElapsedTime = state.timerState.elapsedTime()
                if (state.elapsedTime < state.maxTime && newElapsedTime >= state.maxTime) {
                  soundEffectController.playTimerRunsOut()
                  scalaJsApiClient.doTeamOrQuizStateUpdate(
                    ToggleTimerPaused(timerRunningValue = Some(false))
                  )
                }
                state.copy(elapsedTime = newElapsedTime)
              } else {
                state
              }
            }.runNow()
          },
          /* timeout in millis */ 10,
        )
      )

      Callback.empty
    }
    override def willUnmount(props: Props, state: State): Callback = {
      for (handle <- intervalHandle) dom.window.clearInterval(handle)
      Callback.empty
    }

    private def formatDuration(duration: Duration): String = {
      val seconds = duration.getSeconds()
      "%d:%02d".format(seconds / 60, seconds % 60)
    }
  }
}
