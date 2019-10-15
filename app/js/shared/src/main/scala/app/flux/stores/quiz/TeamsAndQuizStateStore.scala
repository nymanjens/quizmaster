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

  def goToPreviousStep(): Future[Unit] = async {
    val modifications =
      await(stateFuture).maybeQuizState match {
        case None => Seq() // Do nothing
        case Some(quizState) =>
          quizState.question match {
            case None if quizState.roundIndex == 0 =>
              // Go back to setup
              Seq(EntityModification.createRemove(quizState))
            case None =>
              // Go to end of last round
              val newRoundIndex = quizState.roundIndex - 1
              Seq(
                EntityModification.createUpdateAllFields(
                  quizState.copy(
                    roundIndex = newRoundIndex,
                    questionIndex = quizConfig.rounds(newRoundIndex).questions.size - 1,
                    showSolution = true,
                  )))
            case Some(question) if !quizState.showSolution =>
              // Go to previous question
              Seq(
                EntityModification.createUpdateAllFields(
                  quizState.copy(
                    questionIndex = quizState.questionIndex - 1,
                    showSolution = true,
                  )))
            case Some(question) if quizState.showSolution =>
              // Hide solution
              Seq(
                EntityModification.createUpdateAllFields(
                  quizState.copy(
                    showSolution = false,
                  )))
          }
      }

    await(entityAccess.persistModifications(modifications))
  }

  def goToNextStep(): Future[Unit] = async {
    val modifications =
      await(stateFuture).maybeQuizState match {
        case None =>
          Seq(
            EntityModification.Add(
              QuizState(
                roundIndex = 0,
              )))
        case Some(quizState) =>
          quizState.question match {
            case None =>
              if (quizState.round.questions.isEmpty) {
                // Go to next round
                Seq(
                  EntityModification.createUpdateAllFields(
                    QuizState(
                      roundIndex = quizState.roundIndex + 1,
                    )))
              } else {
                // Go to first question
                Seq(
                  EntityModification.createUpdateAllFields(
                    quizState.copy(
                      questionIndex = 0,
                      showSolution = false,
                    )))
              }
            case Some(question) if !quizState.showSolution =>
              // Go to solution
              Seq(
                EntityModification.createUpdateAllFields(
                  quizState.copy(
                    showSolution = true,
                  )))
            case Some(question) if quizState.showSolution =>
              if (quizState.questionIndex == quizState.round.questions.size - 1) {
                // Go to next round
                Seq(
                  EntityModification.createUpdateAllFields(
                    QuizState(
                      roundIndex = quizState.roundIndex + 1,
                    )))
              } else {
                // Go to next question
                Seq(
                  EntityModification.createUpdateAllFields(
                    quizState.copy(
                      questionIndex = quizState.questionIndex + 1,
                      showSolution = false,
                    )))
              }
          }
      }

    await(entityAccess.persistModifications(modifications))
  }
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
