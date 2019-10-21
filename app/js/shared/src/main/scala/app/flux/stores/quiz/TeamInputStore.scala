package app.flux.stores.quiz

import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore.State
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.Submission
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.StateStore
import hydro.models.access.JsEntityAccess

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TeamInputStore(
    implicit entityAccess: JsEntityAccess,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    gamepadStore: GamepadStore,
    soundEffectController: SoundEffectController,
) extends StateStore[State] {
  private var _state: State = State.nullInstance
  gamepadStore.register(GamepadStoreListener)

  override def state: State = _state

  private def setState(newState: State): Unit = {
    if (newState != _state) {
      _state = newState
      Future(invokeStateUpdateListeners())
    }
  }

  private object GamepadStoreListener extends StateStore.Listener {
    override def onStateUpdate(): Unit = {
      val TeamsAndQuizStateStore.State(teams, quizState) = teamsAndQuizStateStore.stateOrEmpty

      setState(
        State(
          teamIdToGamepadState = (teams.map(_.id) zip gamepadStore.state.gamepads).toMap
            .withDefaultValue(GamepadState.nullInstance)))

      if (quizState.canSubmitResponse) {
        val question = quizState.maybeQuestion.get

        for (team <- teams) {
          val gamepadState = _state.teamIdToGamepadState(team.id)
          val teamHasAlreadyAnswered = quizState.submissions.exists(_.teamId == team.id)

          if (!teamHasAlreadyAnswered) {
            question.isMultipleChoice match {
              case true =>
                if (gamepadState.arrowPressed.isDefined) {
                  val arrow = gamepadState.arrowPressed.get
                  val alreadyAnsweredCorrectly = quizState.submissions.exists(submission =>
                    question.isCorrectAnswerIndex(submission.maybeAnswerIndex.get))
                  val tooLate = alreadyAnsweredCorrectly && question.onlyFirstGainsPoints
                  if (!tooLate) {
                    soundEffectController.playNewSubmission()
                    teamsAndQuizStateStore.addSubmission(
                      Submission(
                        teamId = team.id,
                        maybeAnswerIndex = Some(arrow.answerIndex),
                      )
                    )
                  }
                }
              case false =>
                if (gamepadState.anyButtonPressed) {
                  soundEffectController.playNewSubmission()
                  teamsAndQuizStateStore.addSubmission(
                    Submission(
                      teamId = team.id,
                    )
                  )
                }
            }
          }
        }
      }
    }
  }
}

object TeamInputStore {
  case class State(
      teamIdToGamepadState: Map[Long, GamepadState] = Map().withDefaultValue(GamepadState.nullInstance),
  )
  object State {
    def nullInstance = State()
  }
}
