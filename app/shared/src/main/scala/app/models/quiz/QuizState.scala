package app.models.quiz

import hydro.common.time.JavaTimeImplicits._
import java.time.Duration
import java.time.Instant

import scala.collection.immutable.Seq
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState.Submission
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

  def canSubmitResponse(implicit quizConfig: QuizConfig, clock: Clock): Boolean = {
    maybeQuestion match {
      case None => false
      case Some(question) =>
        val submissionAreOpen = question.submissionAreOpen(questionProgressIndex)
        val submissionIsHinderedByTimer =
          if (question.shouldShowTimer(questionProgressIndex))
            // !timerState.timerRunning || // Note: Allowing submissions even when paused to allow submissions while
            // another team is answering
            timerState.hasFinished(question.maybeMaxTime.get)
          else false

        submissionAreOpen && !submissionIsHinderedByTimer
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

  case class Submission(teamId: Long, maybeAnswerIndex: Option[Int] = None)
}
