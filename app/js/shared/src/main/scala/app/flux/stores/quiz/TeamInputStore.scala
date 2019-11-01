package app.flux.stores.quiz

import app.flux.controllers.SoundEffectController
import app.flux.router.AppPages
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore.State
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.Submission
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.SerializingTaskQueue
import hydro.flux.action.Action
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.flux.stores.StateStore
import hydro.models.access.JsEntityAccess

import scala.collection.immutable.Seq
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

  private var currentPage: Page = _
  private var _state: State = State.nullInstance

  /**
    * Queue that processes a single task at once to avoid concurrent update issues (on the same client tab).
    */
  private val updateStateQueue: SerializingTaskQueue = SerializingTaskQueue.create()

  gamepadStore.register(GamepadStoreListener)
  dispatcher.registerPartialSync(dispatcherListener)

  override def state: State = _state

  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case StandardActions.SetPageLoadingState( /* isLoading = */ _, currentPage) =>
      this.currentPage = currentPage
  }

  private def setState(newState: State): Unit = {
    if (newState != _state) {
      _state = newState
      Future(invokeStateUpdateListeners())
    }
  }

  private def onRelevantPageForSubmissions: Boolean = currentPage == AppPages.Quiz

  private object GamepadStoreListener extends StateStore.Listener {
    override def onStateUpdate(): Unit =
      updateStateQueue.schedule {
        val TeamsAndQuizStateStore.State(teams, quizState) = teamsAndQuizStateStore.stateOrEmpty

        setState(
          State(teamIdToGamepadState = (teams.map(_.id) zip gamepadStore.state.gamepads).toMap
            .withDefaultValue(GamepadState.nullInstance)))

        if (onRelevantPageForSubmissions && quizState.canSubmitResponse) {
          maybeAddSubmissions(teams, quizState)
        } else {
          Future.successful((): Unit)
        }
      }

    private def maybeAddSubmissions(teams: Seq[Team], quizState: QuizState): Future[Seq[Unit]] = {
      Future.sequence(
        for (team <- teams) yield maybeAddSubmission(team, quizState)
      )
    }

    private def maybeAddSubmission(team: Team, quizState: QuizState): Future[Unit] = {
      val question = quizState.maybeQuestion.get
      val gamepadState = _state.teamIdToGamepadState(team.id)
      val teamHasAlreadyAnswered = quizState.submissions.exists(_.teamId == team.id)

      if (teamHasAlreadyAnswered) {
        Future.successful((): Unit)
      } else {
        question.isMultipleChoice match {
          case true =>
            if (gamepadState.arrowPressed.isDefined) {
              val arrow = gamepadState.arrowPressed.get
              val alreadyAnsweredCorrectly = quizState.submissions.exists(submission =>
                question.isCorrectAnswerIndex(submission.maybeAnswerIndex.get))
              val tooLate = alreadyAnsweredCorrectly && question.onlyFirstGainsPoints
              if (tooLate) {
                Future.successful((): Unit)
              } else {
                val submissionIsCorrect = question.isCorrectAnswerIndex(arrow.answerIndex)
                if (question.onlyFirstGainsPoints) {
                  soundEffectController.playRevealingSubmission(correct = submissionIsCorrect)
                } else {
                  soundEffectController.playNewSubmission()
                }
                teamsAndQuizStateStore.addSubmission(
                  Submission(
                    teamId = team.id,
                    maybeAnswerIndex = Some(arrow.answerIndex),
                  ),
                  resetTimer = question.isInstanceOf[Question.Double],
                  pauseTimer = question.onlyFirstGainsPoints && submissionIsCorrect,
                )
              }
            } else {
              Future.successful((): Unit)
            }
          case false =>
            if (gamepadState.anyButtonPressed) {
              soundEffectController.playNewSubmission()
              teamsAndQuizStateStore.addSubmission(
                Submission(
                  teamId = team.id,
                ),
                pauseTimer = question.onlyFirstGainsPoints,
              )
            } else {
              Future.successful((): Unit)
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
