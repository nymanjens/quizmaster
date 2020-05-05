package app.models.quiz

import hydro.common.time.JavaTimeImplicits._
import java.time.Duration
import java.time.Instant

import scala.collection.immutable.Seq
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState.GeneralQuizSettings
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.QuizState.TimerState
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class QuizState(
    /**
      * Number from -1 to `rounds.size`. A value of -1 means that the quiz has not started yet. `rounds.size` means
      * that the quiz has finished.
      */
    roundIndex: Int = -1,
    /** Number from -1 to `questions.size - 1`. A value of -1 means that the round name should be shown. */
    questionIndex: Int = -1,
    /** Number from 0 to `questions.progressStepsCount - 1`. */
    questionProgressIndex: Int = 0,
    timerState: TimerState = TimerState.nullInstance,
    submissions: Seq[Submission] = Seq(),
    imageIsEnlarged: Boolean = false,
    generalQuizSettings: GeneralQuizSettings = GeneralQuizSettings.nullInstance,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  // **************** Implementation of interface **************** //
  override val idOption: Option[Long] = Some(QuizState.onlyPossibleId)
  override def withId(id: Long) = {
    require(id == QuizState.onlyPossibleId)
    this
  }
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)

  // **************** Additional public API **************** //
  def round(implicit quizConfig: QuizConfig, i18n: I18n): Round = {
    if (roundIndex < 0) {
      Round(name = i18n("app.welcome"), questions = Seq())
    } else if (roundIndex < quizConfig.rounds.size) {
      quizConfig.rounds(roundIndex)
    } else {
      Round(name = i18n("app.end-of-the-quiz"), questions = Seq())
    }
  }

  def maybeQuestion(implicit quizConfig: QuizConfig): Option[Question] = {
    if (questionIndex == -1) {
      None
    } else {
      Some(quizConfig.rounds(roundIndex).questions(questionIndex))
    }
  }

  def quizIsBeingSetUp: Boolean = roundIndex < 0
  def quizHasEnded(implicit quizConfig: QuizConfig): Boolean = roundIndex >= quizConfig.rounds.size

  def canAnyTeamSubmitResponse(implicit quizConfig: QuizConfig, clock: Clock): Boolean = {
    maybeQuestion match {
      case None => false
      case Some(question) =>
        val submissionAreOpen = question.submissionAreOpen(questionProgressIndex)
        val hinderedByTimer =
          if (question.shouldShowTimer(questionProgressIndex))
            !timerState.timerRunning || timerState.hasFinished(question.maxTime)
          else false
        lazy val earlierSubmissionFinishedTheQuestion = {
          val alreadyAnsweredCorrectly = submissions.exists(_.isCorrectAnswer == Some(true))
          question.onlyFirstGainsPoints && alreadyAnsweredCorrectly
        }

        submissionAreOpen && !hinderedByTimer && !earlierSubmissionFinishedTheQuestion
    }
  }
  def canSubmitResponse(team: Team)(implicit quizConfig: QuizConfig, clock: Clock): Boolean = {
    maybeQuestion match {
      case None => false
      case Some(question) =>
        lazy val earlierTeamSubmission: Option[Submission] = submissions.find(_.teamId == team.id)
        lazy val blockedByEarlierSubmissionOfSameTeam = {
          if (question.isMultipleChoice) {
            if (question.onlyFirstGainsPoints) {
              // Team cannot change their minds because they already know their previous answer was wrong
              earlierTeamSubmission.isDefined
            } else {
              // Allow teams to change their minds while the timer is running
              false
            }
          } else { // Not multiple choice
            if (question.onlyFirstGainsPoints) {
              // Allow multiple guesses while the timer is running, but not the same team twice in a row
              val blockedBecauseAdjacentSubmission = submissions.lastOption.exists(_.teamId == team.id)
              blockedBecauseAdjacentSubmission
            } else {
              // Allow teams to change their minds while the timer is running if they filled in a free-text answer.
              // Teams that pressed a button to indicate that they are done, can only do so once.
              earlierTeamSubmission.exists(_.value == SubmissionValue.PressedTheOneButton)
            }
          }
        }

        canAnyTeamSubmitResponse && !blockedByEarlierSubmissionOfSameTeam
    }
  }
}

object QuizState {
  implicit val Type: EntityType[QuizState] = EntityType()

  val onlyPossibleId: Long = 1
  val nullInstance: QuizState = QuizState()

  def tupled = (this.apply _).tupled

  case class TimerState(
      lastSnapshotInstant: Instant = Instant.EPOCH,
      lastSnapshotElapsedTime: Duration = Duration.ZERO,
      timerRunning: Boolean = false,
  ) {

    def hasFinished(maxTime: Duration)(implicit clock: Clock): Boolean = {
      elapsedTime() > maxTime
    }

    def elapsedTime()(implicit clock: Clock): Duration = {
      if (timerRunning) {
        lastSnapshotElapsedTime + (clock.nowInstant - lastSnapshotInstant)
      } else {
        lastSnapshotElapsedTime
      }
    }
  }
  object TimerState {
    val nullInstance = TimerState()

    def createStarted()(implicit clock: Clock): TimerState = TimerState(
      lastSnapshotInstant = clock.nowInstant,
      lastSnapshotElapsedTime = Duration.ZERO,
      timerRunning = true,
    )
  }

  case class Submission(
      id: Long,
      teamId: Long,
      value: SubmissionValue,
      // If none, there is no information available to make an estimation of correctness
      isCorrectAnswer: Option[Boolean],
  )
  object Submission {
    sealed abstract class SubmissionValue
    object SubmissionValue {
      case object PressedTheOneButton extends SubmissionValue
      case class MultipleChoiceAnswer(answerIndex: Int) extends SubmissionValue
      case class FreeTextAnswer(answerString: String) extends SubmissionValue
    }
  }

  case class GeneralQuizSettings(
      showAnswers: Boolean = true,
      answerBulletType: AnswerBulletType = AnswerBulletType.Arrows,
  )
  object GeneralQuizSettings {
    val nullInstance = GeneralQuizSettings()

    sealed trait AnswerBulletType
    object AnswerBulletType {
      case object Arrows extends AnswerBulletType
      case object Characters extends AnswerBulletType
    }
  }
}
