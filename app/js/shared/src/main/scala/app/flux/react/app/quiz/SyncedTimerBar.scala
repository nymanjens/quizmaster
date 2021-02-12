package app.flux.react.app.quiz

import java.time.Duration

import app.api.ScalaJsApi.TeamOrQuizStateUpdate._
import app.api.ScalaJsApiClient
import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.QuizState.TimerState
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import hydro.common.time.JavaTimeImplicits._
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.ReactVdomUtils.<<
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

final class SyncedTimerBar(implicit
    clock: Clock,
    i18n: I18n,
    soundEffectController: SoundEffectController,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    scalaJsApiClient: ScalaJsApiClient,
    quizConfig: QuizConfig,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(showMasterData: Boolean): VdomElement = {
    component(Props(showMasterData))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(quizState = teamsAndQuizStateStore.stateOrEmpty.quizState),
      )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      showMasterData: Boolean
  )
  protected case class State(
      elapsedTime: Duration = Duration.ZERO,
      quizState: QuizState = QuizState.nullInstance,
  ) {
    def timerState: TimerState = {
      quizState.timerState
    }
    def maxTime: Duration = {
      quizState.maybeQuestion match {
        case Some(q) => q.maxTime
        case None    => Duration.ZERO
      }
    }
    def timerIsEnabled: Boolean = {
      quizState.maybeQuestion match {
        case Some(q) => q.shouldShowTimer(quizState.questionProgressIndex)
        case None    => false
      }
    }
  }

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with WillUnmount {

    private var intervalHandle: Option[Int] = None

    override def render(props: Props, state: State): VdomElement = logExceptions {
      if (state.timerIsEnabled) {
        renderTimerCountingDown(state)
      } else {
        renderQuizProgress(props, state)
      }
    }

    private def renderTimerCountingDown(state: State): VdomElement = {
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
    }

    private def renderQuizProgress(props: Props, state: State): VdomElement = {
      val allQuestions = quizConfig.rounds.flatMap(_.questions)
      val precedingAndCurrentQuestion = {
        for {
          (round, roundIndex) <- quizConfig.rounds.zipWithIndex
          if roundIndex <= state.quizState.roundIndex
          (question, questionIndex) <- round.questions.zipWithIndex
          if roundIndex < state.quizState.roundIndex || questionIndex <= state.quizState.questionIndex
        } yield question
      }

      val totalNumberOfQuestions = allQuestions.size
      val totalMaxDuration = allQuestions.map(_.maxTime).sum
      val doneNumberOfQuestions = precedingAndCurrentQuestion.size
      val doneMaxDuration = precedingAndCurrentQuestion.map(_.maxTime).sum

      <.div(
        ^.className := "synced-timer-bar",
        Bootstrap.ProgressBar(
          fraction = doneMaxDuration.toMillis * 1.0 / totalMaxDuration.toMillis,
          variant = Variant.info,
          label = <.div(
            ^.className := "synced-timer-bar-label",
            s"$doneNumberOfQuestions / $totalNumberOfQuestions ${i18n("app.questions")} " +
              s"(${doneMaxDuration.toMinutes} / ${totalMaxDuration.toMinutes} ${i18n("app.minutes")})",
            " • ",
            roundProgress(state.quizState, props.showMasterData),
          ),
        ),
      )
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

    private def roundProgress(quizState: QuizState, showMasterData: Boolean): VdomNode = {
      implicit val _: QuizState = quizState

      quizState match {
        case _ if quizState.quizIsBeingSetUp =>
          <.span()
        case _ if quizState.quizHasEnded =>
          <.span()
        case _ =>
          <.span(
            i18n("app.round-i-of-n", quizState.roundIndex + 1, quizConfig.rounds.size),
            s" (${quizState.round.name}). ",
            if (quizState.maybeQuestion.isDefined) {
              i18n("app.question-i-of-n", quizState.questionIndex + 1, quizState.round.questions.size)
            } else {
              s"${quizState.round.questions.size} ${i18n("app.questions")}"
            },
            ". ",
            <<.ifThen(showMasterData) {
              <<.ifDefined(quizState.maybeQuestion) { question =>
                {
                  for (i <- 0 to question.progressStepsCount - 1) yield {
                    Bootstrap.FontAwesomeIcon("circle")(
                      ^.key := i,
                      ^.className := (if (i <= quizState.questionProgressIndex) "seen" else "unseen"),
                    )
                  }
                }.toVdomArray
              }
            },
          )
      }
    }

    private def formatDuration(duration: Duration): String = {
      val seconds = duration.getSeconds()
      "%d:%02d".format(seconds / 60, seconds % 60)
    }
  }
}
