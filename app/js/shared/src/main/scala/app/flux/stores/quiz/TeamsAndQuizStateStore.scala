package app.flux.stores.quiz

import app.api.ScalaJsApiClient
import app.flux.action.AppActions
import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.TimerState
import app.models.quiz.export.ExportImport
import app.models.quiz.export.ExportImport.FullState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.CollectionUtils.maybeGet
import hydro.common.I18n
import hydro.common.SerializingTaskQueue
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.access.ModelField
import hydro.models.modification.EntityModification
import japgolly.scalajs.react.CallbackTo

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
      scalaJsApiClient.addSubmission(teamId, submissionValue)
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
    val FullState(teamsToImport, quizStateToImport) = ExportImport.importFromString(importString)
    println(s"  Importing: teams = ${teamsToImport}, quizState = ${quizStateToImport}")

    updateStateQueue.schedule {
      async {
        val deleteExistingTeams = await(stateFuture).teams.map(team => EntityModification.createRemove(team))
        val importTeams = teamsToImport.map(team => EntityModification.createAddWithRandomId(team))
        val addOrUpdateQuizState = Seq(
          EntityModification.Add(quizStateToImport),
          EntityModification.createUpdateAllFields(quizStateToImport),
        )
        await(entityAccess.persistModifications(deleteExistingTeams ++ importTeams ++ addOrUpdateQuizState))
      }
    }
  }

  def updateName(team: Team, newName: String): Future[Unit] = updateStateQueue.schedule {
    entityAccess.persistModifications(EntityModification.createUpdateAllFields(team.copy(name = newName)))
  }

  def updateScore(team: Team, scoreDiff: Int): Future[Unit] = updateStateQueue.schedule {
    async {
      if (scoreDiff != 0) {
        val oldScore = await(stateFuture).teams.find(_.id == team.id).get.score
        val newScore = oldScore + scoreDiff
        await(
          entityAccess.persistModifications(
            EntityModification.createUpdate(team.copy(score = newScore), Seq(ModelFields.Team.score))))
      }
    }
  }

  def updateScore(teamIndex: Int, scoreDiff: Int): Future[Unit] = async {
    val team = await(stateFuture).teams(teamIndex)
    await(updateScore(team, scoreDiff))
  }

  def deleteTeam(team: Team): Future[Unit] = updateStateQueue.schedule {
    entityAccess.persistModifications(EntityModification.createRemove(team))
  }

  def goToPreviousStep(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(StateUpsertHelper.goToPreviousStepUpdate)
      .map(_ => (): Unit)
  }

  def goToNextStep(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(StateUpsertHelper.goToNextStepUpdate)
      .map(_ => (): Unit)
  }

  /**
    * Go to the start of the current question if it's not already there or the start of the previous question
    * otherwise
    */
  def goToPreviousQuestion(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(
        StateUpsertHelper.goToPreviousQuestionUpdate)
      .map(_ => (): Unit)
  }

  def goToNextQuestion(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(StateUpsertHelper.goToNextQuestionUpdate)
      .map(_ => (): Unit)
  }

  /**
    * Go to the start of the current round if it's not already there or the start of the previous round
    * otherwise
    */
  def goToPreviousRound(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(
        StateUpsertHelper.goToPreviousRoundUpdate)
      .map(_ => (): Unit)
  }
  def goToNextRound(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields)(StateUpsertHelper.goToNextRoundUpdate)
      .map(_ => (): Unit)
  }

  def resetCurrentQuestion(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(Seq(ModelFields.QuizState.timerState, ModelFields.QuizState.submissions))(
        _.copy(timerState = TimerState.createStarted(), submissions = Seq()))
      .map(_ => (): Unit)
  }

  def toggleImageIsEnlarged(): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(Seq(ModelFields.QuizState.imageIsEnlarged)) { oldState =>
        oldState.copy(imageIsEnlarged = !oldState.imageIsEnlarged)
      }
      .map(_ => (): Unit)
  }

  def setShowAnswers(showAnswers: Boolean): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(Seq(ModelFields.QuizState.generalQuizSettings)) { oldState =>
        oldState.copy(generalQuizSettings = oldState.generalQuizSettings.copy(showAnswers = showAnswers))
      }
      .map(_ => (): Unit)
  }

  def setAnswerBulletType(answerBulletType: AnswerBulletType): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(Seq(ModelFields.QuizState.generalQuizSettings)) { oldState =>
        oldState.copy(
          generalQuizSettings = oldState.generalQuizSettings.copy(answerBulletType = answerBulletType))
      }
      .map(_ => (): Unit)
  }

  def togglePaused(timerRunningValue: Option[Boolean] = None): Future[Unit] = updateStateQueue.schedule {
    StateUpsertHelper
      .doQuizStateUpsert(StateUpsertHelper.progressionRelatedFields) { state =>
        val timerState = state.timerState
        state.copy(timerState = TimerState(
          lastSnapshotInstant = clock.nowInstant,
          lastSnapshotElapsedTime = timerState.elapsedTime(),
          timerRunning = timerRunningValue getOrElse (!timerState.timerRunning),
        ))
      }
      .map(_ => (): Unit)
  }

  private object StateUpsertHelper {

    val progressionRelatedFields: Seq[ModelField[_, QuizState]] = Seq(
      ModelFields.QuizState.roundIndex,
      ModelFields.QuizState.questionIndex,
      ModelFields.QuizState.questionProgressIndex,
      ModelFields.QuizState.timerState,
      ModelFields.QuizState.imageIsEnlarged,
      ModelFields.QuizState.submissions
    )

    /** Returns true if something changed. */
    def doQuizStateUpsert(fieldMasks: Seq[ModelField[_, QuizState]])(
        update: QuizState => QuizState): Future[Boolean] =
      async {
        val quizState = await(stateFuture).quizState

        // Optimization: Only fetch state via (non-cached) entityAccess if there is a reason
        // to think it may have to be added
        if (quizState == QuizState.nullInstance) {
          val maybeQuizState = await(
            entityAccess
              .newQuery[QuizState]()
              .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId))

          maybeQuizState match {
            case None =>
              await(entityAccess.persistModifications(EntityModification.Add(update(QuizState.nullInstance))))
              true
            case Some(quizState2) =>
              val updatedState = update(quizState2)
              if (quizState2 == updatedState) {
                false
              } else {
                await(
                  entityAccess.persistModifications(
                    EntityModification.createUpdate(updatedState, fieldMasks.toVector)))
                true
              }
          }
        } else {
          val updatedState = update(quizState)

          if (quizState == updatedState) {
            false
          } else {
            await(
              entityAccess.persistModifications(
                EntityModification.createUpdate(updatedState, fieldMasks.toVector)))
            true
          }
        }
      }

    def goToPreviousStepUpdate(quizState: QuizState): QuizState = {
      implicit val implicitOldQuizState = quizState

      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the end of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              val newRound = quizConfig.rounds(newRoundIndex)
              quizState.copy(
                roundIndex = newRoundIndex,
                questionIndex = newRound.questions.size - 1,
                questionProgressIndex = newRound.questions.lastOption.map(_.maxProgressIndex) getOrElse 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) if quizState.questionProgressIndex == 0 =>
              // Go to the end of the previous question
              val newQuestionIndex = quizState.questionIndex - 1
              quizState.copy(
                questionIndex = newQuestionIndex,
                questionProgressIndex =
                  maybeGet(quizState.round.questions, newQuestionIndex)
                    .map(_.maxProgressIndex) getOrElse 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) if quizState.questionProgressIndex > 0 =>
              // Decrement questionProgressIndex
              quizState.copy(
                questionProgressIndex = quizState.questionProgressIndex - 1,
                timerState = TimerState.createStarted(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextStepUpdate(quizState: QuizState): QuizState = {
      implicit val implicitOldQuizState = quizState

      quizState.maybeQuestion match {
        case None =>
          goToNextQuestionUpdate(quizState)
        case Some(question) if quizState.questionProgressIndex < question.maxProgressIndex =>
          // Add and remove points
          if (quizState.questionProgressIndex == question.maxProgressIndex - 1) {
            Future(addOrRemovePoints(quizState))
          }

          // Increment questionProgressIndex
          quizState.copy(
            questionProgressIndex = quizState.questionProgressIndex + 1,
            timerState = TimerState.createStarted(),
            imageIsEnlarged = false,
          )
        case Some(question) if quizState.questionProgressIndex == question.maxProgressIndex =>
          goToNextQuestionUpdate(quizState)
      }
    }

    def goToPreviousQuestionUpdate(quizState: QuizState): QuizState = {
      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the start of the last question of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              val newRound = quizConfig.rounds(newRoundIndex)
              quizState.copy(
                roundIndex = newRoundIndex,
                questionIndex = newRound.questions.size - 1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) if quizState.questionProgressIndex == 0 =>
              // Go to the start of the previous question
              val newQuestionIndex = quizState.questionIndex - 1
              quizState.copy(
                questionIndex = newQuestionIndex,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) if quizState.questionProgressIndex > 0 =>
              // Go to the start of the question
              quizState.copy(
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextQuestionUpdate(quizState: QuizState): QuizState = {
      quizState.maybeQuestion match {
        case None =>
          if (quizState.round.questions.isEmpty) {
            // Go to next round
            goToNextRoundUpdate(quizState)
          } else {
            // Go to first question
            quizState.copy(
              questionIndex = 0,
              questionProgressIndex = 0,
              timerState = TimerState.createStarted(),
              submissions = Seq(),
              imageIsEnlarged = false,
            )
          }
        case Some(question) =>
          if (quizState.questionIndex == quizState.round.questions.size - 1) {
            // Go to next round
            goToNextRoundUpdate(quizState)
          } else {
            // Go to next question
            quizState.copy(
              questionIndex = quizState.questionIndex + 1,
              questionProgressIndex = 0,
              timerState = TimerState.createStarted(),
              submissions = Seq(),
              imageIsEnlarged = false,
            )
          }
      }
    }

    def goToPreviousRoundUpdate(quizState: QuizState): QuizState = {
      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the start of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              quizState.copy(
                roundIndex = newRoundIndex,
                questionIndex = -1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) =>
              // Go to the start of the current round
              quizState.copy(
                questionIndex = -1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextRoundUpdate(quizState: QuizState): QuizState = {
      quizState.copy(
        roundIndex = quizState.roundIndex + 1,
        questionIndex = -1,
        questionProgressIndex = 0,
        timerState = TimerState.createStarted(),
        submissions = Seq(),
        imageIsEnlarged = false,
      )
    }

    private def addOrRemovePoints(quizState: QuizState): Unit = {
      val question = quizState.maybeQuestion.get
      var firstCorrectAnswerSeen = false
      for (submission <- quizState.submissions) {
        val scoreDiff = {
          if (submission.value.isScorable) {
            val correct = question.isCorrectAnswer(submission.value)
            if (correct) {
              if (firstCorrectAnswerSeen) {
                question.pointsToGain
              } else {
                firstCorrectAnswerSeen = true
                question.pointsToGainOnFirstAnswer
              }
            } else {
              question.pointsToGainOnWrongAnswer
            }
          } else {
            0
          }
        }
        val team = stateOrEmpty.teams.find(_.id == submission.teamId).get
        updateScore(team, scoreDiff = scoreDiff)
      }
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
