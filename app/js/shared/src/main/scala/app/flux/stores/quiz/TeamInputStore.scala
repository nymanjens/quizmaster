package app.flux.stores.quiz

import app.api.ScalaJsApi.TeamOrQuizStateUpdate.AddSubmission
import app.api.ScalaJsApiClient
import app.flux.router.AppPages
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.TeamInputStore.State
import app.models.access.ModelFields
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission.SubmissionValue
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
    scalaJsApiClient: ScalaJsApiClient,
) extends StateStore[State] {

  private var currentPage: Page = _
  private var _state: State = State.nullInstance

  /**
    * Queue that processes a single task at once to avoid concurrent update issues (on the same client tab).
    */
  private val updateStateQueue: SerializingTaskQueue = SerializingTaskQueue.create()

  gamepadStore.register(GamepadStoreListener)
  dispatcher.registerPartialSync(dispatcherListener)

  // **************** Implementation of StateStore methods **************** //
  override def state: State = _state

  // **************** Additional public API **************** //
  def alertTeam(teamId: Long): Unit = async {
    val allTeams = await(
      entityAccess
        .newQuery[Team]()
        .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
        .data())

    for (team <- allTeams.find(_.id == teamId)) {
      gamepadStore.rumble(gamepadIndex = allTeams.indexOf(team))
    }
  }

  // **************** Private helper methods and objects **************** //
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
        resultFuture = resultFuture.flatMap(_ => maybeAddSubmission(team))
      }
      resultFuture
    }

    private def maybeAddSubmission(team: Team): Future[Unit] = async {
      val quizState = await(
        entityAccess
          .newQuery[QuizState]()
          .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)) getOrElse QuizState.nullInstance

      if (onRelevantPageForSubmissions && quizState.canSubmitResponse(team)) {
        val question = quizState.maybeQuestion.get
        val gamepadState = _state.teamIdToGamepadState(team.id)

        val submissinoValue = {
          if (question.isMultipleChoice) {
            if (gamepadState.arrowPressed.isDefined) {
              Some(SubmissionValue.MultipleChoiceAnswer(gamepadState.arrowPressed.get.answerIndex))
            } else {
              None
            }
          } else { // Not multiple choice
            if (gamepadState.anyButtonPressed) {
              Some(SubmissionValue.PressedTheOneButton)
            } else {
              None
            }
          }
        }

        if (submissinoValue.isDefined) {
          await(scalaJsApiClient.doTeamOrQuizStateUpdate(AddSubmission(team.id, submissinoValue.get)))
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
