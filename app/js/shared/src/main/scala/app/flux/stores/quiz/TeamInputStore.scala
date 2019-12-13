package app.flux.stores.quiz

import app.flux.controllers.SoundEffectController
import app.flux.router.AppPages
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore.State
import app.models.access.ModelFields
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
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Random

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
        async {
          // Re-fetching so that it is sure to have the latest value
          val teams = await(
            entityAccess
              .newQuery[Team]()
              .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
              .data())

          setState(
            State(teamIdToGamepadState = (teams.map(_.id) zip gamepadStore.state.gamepads).toMap
              .withDefaultValue(GamepadState.nullInstance)))

          await(maybeAddSubmissions(teams))
        }
      }

    private def maybeAddSubmissions(teams: Seq[Team]): Future[Unit] = {
      // Randomly shuffle the order in which we handle the teams so it's not always the same team that
      // gets the advantage.
      val randomTeams = Random.shuffle(teams)

      var resultFuture: Future[Unit] = Future.successful((): Unit)
      for (team <- randomTeams) {
        resultFuture = resultFuture.flatMap(_ => maybeAddSubmission(team, teams))
      }
      resultFuture
    }

    private def maybeAddSubmission(team: Team, allTeams: Seq[Team]): Future[Unit] = async {
      val quizState = await(
        entityAccess
          .newQuery[QuizState]()
          .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)) getOrElse QuizState.nullInstance

      if (onRelevantPageForSubmissions && quizState.canSubmitResponse) {

        val question = quizState.maybeQuestion.get
        val gamepadState = _state.teamIdToGamepadState(team.id)
        def teamHasSubmission(thisTeam: Team): Boolean =
          quizState.submissions.exists(_.teamId == thisTeam.id)
        def allOtherTeamsHaveSubmission = allTeams.filter(_ != team).forall(teamHasSubmission)

        if (question.isMultipleChoice) {
          if (gamepadState.arrowPressed.isDefined) {
            val arrow = gamepadState.arrowPressed.get
            val submissionIsCorrect = question.isCorrectAnswerIndex(arrow.answerIndex)
            val tooLate = {
              val alreadyAnsweredCorrectly = quizState.submissions.exists(submission =>
                question.isCorrectAnswerIndex(submission.maybeAnswerIndex.get))
              alreadyAnsweredCorrectly && question.onlyFirstGainsPoints
            }

            if (tooLate) {
              // do nothing
            } else {
              if (submissionIsCorrect && question.isInstanceOf[Question.Double]) {
                gamepadStore.rumble(gamepadIndex = allTeams.indexOf(team))
              }

              val somethingChanged = await(
                teamsAndQuizStateStore.addSubmission(
                  Submission.createNow(
                    teamId = team.id,
                    maybeAnswerIndex = Some(arrow.answerIndex),
                  ),
                  resetTimer = question.isInstanceOf[Question.Double],
                  pauseTimer =
                    if (question.onlyFirstGainsPoints) submissionIsCorrect else allOtherTeamsHaveSubmission,
                  allowMoreThanOneSubmissionPerTeam = false,
                  removeEarlierDifferentSubmissionBySameTeam = !question.onlyFirstGainsPoints,
                ))

              if (somethingChanged) {
                if (question.onlyFirstGainsPoints) {
                  soundEffectController.playRevealingSubmission(correct = submissionIsCorrect)
                } else {
                  soundEffectController.playNewSubmission()
                }
              }
            }
          }
        } else { // Not multiple choice
          if (gamepadState.anyButtonPressed) {
            val somethingChanged = await(
              teamsAndQuizStateStore.addSubmission(
                Submission.createNow(teamId = team.id),
                pauseTimer = if (question.onlyFirstGainsPoints) true else allOtherTeamsHaveSubmission,
                //allowMoreThanOneSubmissionPerTeam = question.onlyFirstGainsPoints,
                allowMoreThanOneSubmissionPerTeam = false,
              ))

            if (somethingChanged) {
              soundEffectController.playNewSubmission()
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
