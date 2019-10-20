package app.flux.stores.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.TimerState
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.CollectionUtils.maybeGet
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
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

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
  def addEmptyTeam(): Future[Unit] = updateStateQueue.schedule {
    async {
      val maxIndex = await(stateFuture).teams.map(_.index).max
      await(
        entityAccess.persistModifications(
          EntityModification.createAddWithRandomId(
            Team(
              name = "",
              score = 0,
              index = maxIndex + 1,
            ))))
    }
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
    doQuizStateUpdateInternal(
      ModelFields.QuizState.roundIndex,
      ModelFields.QuizState.questionIndex,
      ModelFields.QuizState.questionProgressIndex,
      ModelFields.QuizState.timerState,
      ModelFields.QuizState.submissions,
    ) { quizState =>
      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.question match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to end of last round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              val newRound = quizConfig.rounds(newRoundIndex)
              quizState.copy(
                roundIndex = newRoundIndex,
                questionIndex = newRound.questions.size - 1,
                questionProgressIndex = newRound.questions.lastOption.map(_.maxProgressIndex) getOrElse 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
              )
            case Some(question) if quizState.questionProgressIndex == 0 =>
              // Go to previous question
              val newQuestionIndex = quizState.questionIndex - 1
              quizState.copy(
                questionIndex = newQuestionIndex,
                questionProgressIndex =
                  maybeGet(quizState.round.questions, newQuestionIndex)
                    .map(_.maxProgressIndex) getOrElse 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
              )
            case Some(question) if quizState.questionProgressIndex > 0 =>
              // Decrement questionProgressIndex
              quizState.copy(
                questionProgressIndex = quizState.questionProgressIndex - 1,
                timerState = TimerState.createStarted(),
              )
          }
      }
    }
  }

  def goToNextStep(): Future[Unit] = updateStateQueue.schedule {
    async {
      val maybeQuizState = await(
        entityAccess
          .newQuery[QuizState]()
          .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId))

      maybeQuizState match {
        case None =>
          await(
            entityAccess.persistModifications(
              Seq(
                EntityModification.Add(QuizState(
                  roundIndex = 0,
                  timerState = TimerState.createStarted(),
                )))))

        case Some(_) =>
          await(
            doQuizStateUpdateInternal(
              ModelFields.QuizState.roundIndex,
              ModelFields.QuizState.questionIndex,
              ModelFields.QuizState.questionProgressIndex,
              ModelFields.QuizState.timerState,
              ModelFields.QuizState.submissions,
            ) { quizState =>
              quizState.question match {
                case None =>
                  if (quizState.round.questions.isEmpty) {
                    // Go to next round
                    QuizState(
                      roundIndex = quizState.roundIndex + 1,
                      timerState = TimerState.createStarted(),
                      submissions = Seq(),
                    )
                  } else {
                    // Go to first question
                    quizState.copy(
                      questionIndex = 0,
                      questionProgressIndex = 0,
                      timerState = TimerState.createStarted(),
                      submissions = Seq(),
                    )
                  }
                case Some(question) if quizState.questionProgressIndex < question.maxProgressIndex =>
                  // Increment questionProgressIndex
                  quizState.copy(
                    questionProgressIndex = quizState.questionProgressIndex + 1,
                    timerState = TimerState.createStarted(),
                  )
                case Some(question) if quizState.questionProgressIndex == question.maxProgressIndex =>
                  if (quizState.questionIndex == quizState.round.questions.size - 1) {
                    // Go to next round
                    QuizState(
                      roundIndex = quizState.roundIndex + 1,
                      questionProgressIndex = 0,
                      timerState = TimerState.createStarted(),
                      submissions = Seq(),
                    )
                  } else {
                    // Go to next question
                    quizState.copy(
                      questionIndex = quizState.questionIndex + 1,
                      questionProgressIndex = 0,
                      timerState = TimerState.createStarted(),
                      submissions = Seq(),
                    )
                  }
              }
            }
          )
      }
    }
  }

  def doQuizStateUpdate(fieldMasks: ModelField[_, QuizState]*)(update: QuizState => QuizState): Future[Unit] =
    updateStateQueue.schedule {
      doQuizStateUpdateInternal(fieldMasks: _*)(update)
    }

  private def doQuizStateUpdateInternal(fieldMasks: ModelField[_, QuizState]*)(
      update: QuizState => QuizState): Future[Unit] =
    async {
      val quizState = await(stateFuture).quizState
      val newQuizState = update(quizState)
      await(
        entityAccess.persistModifications(EntityModification.createUpdate(newQuizState, fieldMasks.toVector)))
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
