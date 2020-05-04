package app.flux.stores.quiz

import app.api.ScalaJsApi.TeamOrQuizStateUpdate._
import app.api.ScalaJsApiClient
import app.flux.action.AppActions
import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.common.SerializingTaskQueue
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TeamsAndQuizStateStore(
    implicit entityAccess: JsEntityAccess,
    i18n: I18n,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
    scalaJsApiClient: ScalaJsApiClient,
) extends AsyncEntityDerivedStateStore[State] {

  dispatcher.registerPartialAsync {
    case AppActions.AddSubmission(teamId, submissionValue) =>
      scalaJsApiClient.doTeamOrQuizStateUpdate(AddSubmission(teamId, submissionValue))
  }

  /**
    * Queue that processes a single task at once to avoid concurrent update issues (on the same client tab).
    */
  private val updateStateQueue: SerializingTaskQueue = SerializingTaskQueue.create()

  // **************** Implementation of AsyncEntityDerivedStateStore methods **************** //
  override protected def calculateState(): Future[State] = async {
    State(
      teams = await(
        entityAccess
          .newQuery[Team]()
          .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
          .data()),
      quizState = await(
        entityAccess
          .newQuery[QuizState]()
          .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)) getOrElse QuizState.nullInstance,
    )
  }

  override protected def modificationImpactsState(
      entityModification: EntityModification,
      state: State,
  ): Boolean = true

  // **************** Additional public API: Read methods **************** //
  def stateOrEmpty: State = state getOrElse State.nullInstance

  // **************** Additional public API: Write methods **************** //
  def addTeam(name: String): Future[Team] = updateStateQueue.schedule {
    async {
      val teams = await(stateFuture).teams
      val maxIndex = if (teams.nonEmpty) teams.map(_.index).max else -1
      val modification = EntityModification.createAddWithRandomId(
        Team(
          name = name,
          score = 0,
          index = maxIndex + 1,
        ))
      await(entityAccess.persistModifications(modification))
      modification.entity
    }
  }

  def replaceAllEntitiesByImportString(importString: String): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(ReplaceAllEntitiesByImportString(importString))
  }
  def updateName(team: Team, newName: String): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(UpdateName(team.id, newName))
  }
  def updateScore(team: Team, scoreDiff: Int): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(UpdateScore(teamId = team.id, scoreDiff))
  }
  def updateScore(teamIndex: Int, scoreDiff: Int): Future[Unit] = async {
    val team = await(stateFuture).teams(teamIndex)
    await(updateScore(team, scoreDiff))
  }
  def deleteTeam(team: Team): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(DeleteTeam(teamId = team.id))
  }
  def goToPreviousStep(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToPreviousStep())
  }
  def goToNextStep(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToNextStep())
  }
  def goToPreviousQuestion(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToPreviousQuestion())
  }
  def goToNextQuestion(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToNextQuestion())
  }
  def goToPreviousRound(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToPreviousRound())
  }
  def goToNextRound(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(GoToNextRound())
  }
  def resetCurrentQuestion(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(ResetCurrentQuestion())
  }
  def toggleImageIsEnlarged(): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(ToggleImageIsEnlarged())
  }
  def setShowAnswers(showAnswers: Boolean): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(SetShowAnswers(showAnswers))
  }
  def setAnswerBulletType(answerBulletType: AnswerBulletType): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(SetAnswerBulletType(answerBulletType))
  }
  def togglePaused(timerRunningValue: Option[Boolean] = None): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(ToggleTimerPaused(timerRunningValue))
  }
  def setSubmissionCorrectness(submissionId: Long, isCorrectAnswer: Boolean): Future[Unit] = {
    scalaJsApiClient.doTeamOrQuizStateUpdate(SetSubmissionCorrectness(submissionId, isCorrectAnswer))
  }
}

object TeamsAndQuizStateStore {
  case class State(
      teams: Seq[Team],
      quizState: QuizState,
  )
  object State {
    val nullInstance = State(teams = Seq(), quizState = QuizState.nullInstance)
  }
}
