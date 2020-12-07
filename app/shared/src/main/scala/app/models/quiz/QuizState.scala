package app.models.quiz

import java.lang.Math.abs
import java.time.Duration
import java.time.Instant

import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState.GeneralQuizSettings
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.QuizState.TimerState
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.common.I18n
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq
import scala.util.Random

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
      Round(
        name = quizConfig.title getOrElse i18n("app.welcome"),
        questions = Seq(),
      )
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
              // // Allow multiple guesses while the timer is running, but not the same team twice in a row
              // val blockedBecauseAdjacentSubmission = submissions.lastOption.exists(_.teamId == team.id)
              // blockedBecauseAdjacentSubmission
              false
            } else {
              // Allow teams to change their minds while the timer is running if they filled in a free-text
              // answer. Teams that pressed a button to indicate that they are done, can only do so once.
              earlierTeamSubmission.exists(_.value == SubmissionValue.PressedTheOneButton)
            }
          }
        }

        canAnyTeamSubmitResponse && !blockedByEarlierSubmissionOfSameTeam
    }
  }

  def pointsToGainBySubmission(
      isCorrectAnswer: Option[Boolean],
      submissionId: Long,
      submissionValue: SubmissionValue,
  )(implicit quizConfig: QuizConfig): FixedPointNumber = {
    val previousCorrectSubmissionsExist =
      submissions.takeWhile(_.id != submissionId).exists(_.isCorrectAnswer == Some(true))
    val question = maybeQuestion.get

    question.getPointsToGain(
      submissionValue = Some(submissionValue),
      isCorrect = isCorrectAnswer,
      previousCorrectSubmissionsExist = previousCorrectSubmissionsExist,
    )
  }
}

object QuizState {
  implicit val Type: EntityType[QuizState] = EntityType()

  val onlyPossibleId: Long = 1
  val nullInstance: QuizState = QuizState()

  def tupled = (this.apply _).tupled

  case class TimerState(
      lastSnapshotInstant: Instant,
      lastSnapshotElapsedTime: Duration,
      timerRunning: Boolean,
      // Unique ID that should only change when a song/video should start from the beginning
      uniqueIdOfMediaPlaying: Long,
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
    val nullInstance = TimerState(
      lastSnapshotInstant = Instant.EPOCH,
      lastSnapshotElapsedTime = Duration.ZERO,
      timerRunning = false,
      uniqueIdOfMediaPlaying = 0,
    )

    def createStarted()(implicit clock: Clock): TimerState = TimerState(
      lastSnapshotInstant = clock.nowInstant,
      lastSnapshotElapsedTime = Duration.ZERO,
      timerRunning = true,
      uniqueIdOfMediaPlaying = abs(Random.nextLong),
    )
  }

  case class Submission(
      id: Long,
      teamId: Long,
      value: SubmissionValue,
      // If none, there is no information available to make an estimation of correctness
      isCorrectAnswer: Option[Boolean],
      // The points that this submission will gain / gained.
      points: FixedPointNumber,
      // If true, the `points` were added to the team score
      scored: Boolean,
  )
  object Submission {
    def createUnscoredFromCurrentState(
        id: Long,
        teamId: Long,
        value: SubmissionValue,
        isCorrectAnswer: Option[Boolean],
    )(implicit quizState: QuizState, quizConfig: QuizConfig): Submission = {
      Submission(
        id = EntityModification.generateRandomId(),
        teamId = teamId,
        value = value,
        isCorrectAnswer = isCorrectAnswer,
        points = {
          if (value == SubmissionValue.PressedTheOneButton) {
            // PressedTheOneButton is not automatically scoable, so this would always be zero. However in some
            // cases where answers are found incrementally, it is useful to keep the old points so that they
            // can be further augmented.
            quizState.submissions
              .filter(_.teamId == teamId)
              .lastOption
              .map(_.points) getOrElse FixedPointNumber(0)
          } else {
            quizState.pointsToGainBySubmission(
              isCorrectAnswer = isCorrectAnswer,
              submissionId = id,
              submissionValue = value,
            )
          }
        },
        scored = false,
      )
    }

    sealed abstract class SubmissionValue
    object SubmissionValue {
      case object PressedTheOneButton extends SubmissionValue
      case class MultipleChoiceAnswer(answerIndex: Int) extends SubmissionValue
      case class FreeTextAnswer(answerString: String) extends SubmissionValue
    }
  }

  case class GeneralQuizSettings(
      sortTeamsByScore: Boolean = false,
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
