package app.flux.stores.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.collection.immutable.Seq

final class TeamsAndQuizStateStore(
    implicit entityAccess: JsEntityAccess,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
) extends AsyncEntityDerivedStateStore[State] {

  // **************** Implementation of AsyncEntityDerivedStateStore methods **************** //
  override protected def calculateState(): Future[State] = async {
    State(
      teams = await(
        entityAccess
          .newQuery[Team]()
          .sort(DbQuery.Sorting.ascBy(ModelFields.Team.createTimeMillisSinceEpoch))
          .data()),
      maybeQuizState = await(
        entityAccess.newQuery[QuizState]().findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)),
    )
  }

  override protected def modificationImpactsState(
      entityModification: EntityModification,
      state: State,
  ): Boolean = true

  // **************** Additional public API **************** //
  def stateOrEmpty: State = state getOrElse State.nullInstance

  def addEmptyTeam(): Future[Unit] = {
    entityAccess.persistModifications(
      EntityModification.createAddWithRandomId(
        Team(
          name = "",
          score = 0,
          createTimeMillisSinceEpoch = clock.nowInstant.toEpochMilli,
        )))
  }
  def updateName(team: Team, newName: String): Future[Unit] = {
    entityAccess.persistModifications(EntityModification.createUpdateAllFields(team.copy(name = newName)))
  }
  def updateScore(team: Team, newScore: Int): Future[Unit] = {
    entityAccess.persistModifications(EntityModification.createUpdateAllFields(team.copy(score = newScore)))
  }
  def deleteTeam(team: Team): Future[Unit] = {
    entityAccess.persistModifications(EntityModification.createRemove(team))
  }

  def startQuiz(): Future[Unit] = {
    entityAccess.persistModifications(
      EntityModification.Add(
        QuizState(
          partNumber = 0,
        )
      ))
  }

  def goToPreviousStep(): Future[Unit] = async {
    await(stateFuture).maybeQuizState match {
      case None => // Do nothing
      case Some(quizState) =>
        quizState.question match {
          case None           => ???
          case Some(question) => ???
        }
    }
  }
  def goToNextStep(): Future[Unit] = ???
}

object TeamsAndQuizStateStore {
  case class State(
      teams: Seq[Team],
      maybeQuizState: Option[QuizState],
  )
  object State {
    val nullInstance = State(teams = Seq(), maybeQuizState = None)
  }
}
