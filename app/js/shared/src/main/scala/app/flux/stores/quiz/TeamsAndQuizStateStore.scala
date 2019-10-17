package app.flux.stores.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.SerializingTaskQueue
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.access.ModelField
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
          .sort(DbQuery.Sorting.ascBy(ModelFields.Team.createTimeMillisSinceEpoch))
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
  def addEmptyTeam(): Future[Unit] = updateStateQueue.schedule {
    entityAccess.persistModifications(
      EntityModification.createAddWithRandomId(
        Team(
          name = "",
          score = 0,
          createTimeMillisSinceEpoch = clock.nowInstant.toEpochMilli,
        )))
  }
  def updateName(team: Team, newName: String): Future[Unit] = updateStateQueue.schedule {
    entityAccess.persistModifications(EntityModification.createUpdateAllFields(team.copy(name = newName)))
  }
  def updateScore(team: Team, scoreDiff: Int): Future[Unit] = updateStateQueue.schedule {
    async {
      val oldScore = await(stateFuture).teams.find(_.id == team.id).get.score
      val newScore = oldScore + scoreDiff
      await(
        entityAccess.persistModifications(
          EntityModification.createUpdateAllFields(team.copy(score = newScore))))
    }
  }
  def deleteTeam(team: Team): Future[Unit] = updateStateQueue.schedule {
    entityAccess.persistModifications(EntityModification.createRemove(team))
  }

  def goToPreviousStep(): Future[Unit] = updateStateQueue.schedule {
    async {
      val quizState = await(stateFuture).quizState
      val modifications =
        quizState.roundIndex match {
          case -1 => Seq() // Do nothing
          case _ =>
            quizState.question match {
              case None if quizState.roundIndex == 0 =>
                Seq(EntityModification.createUpdateAllFields(QuizState.nullInstance))
              case None =>
                // Go to end of last round
                val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
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
  }

  def goToNextStep(): Future[Unit] = updateStateQueue.schedule {
    async {
      val maybeQuizState = await(
        entityAccess
          .newQuery[QuizState]()
          .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId))

      val modifications =
        maybeQuizState match {
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

  def doQuizStateUpdate(fieldMask: Seq[ModelField[_, QuizState]])(
      update: QuizState => QuizState): Future[Unit] = updateStateQueue.schedule {
    async {
      val quizState = await(stateFuture).quizState
      val newQuizState = update(quizState)
      await(entityAccess.persistModifications(EntityModification.createUpdate(newQuizState, fieldMask)))
    }
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
