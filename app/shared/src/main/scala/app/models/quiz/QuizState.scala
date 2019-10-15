package app.models.quiz

import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class QuizState(
    /** Number from 0 to `rounds.size - 1` */
    roundIndex: Int,
    /** Number from 0 to `questions.size - 1`. A value of -1 means that the round name should be shown. */
    questionNumber: Int = -1,
    showSolution: Boolean = false,
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
  def round(implicit quizConfig: QuizConfig): Round = quizConfig.rounds(roundIndex)
  def question(implicit quizConfig: QuizConfig): Option[Question] = {
    if (questionNumber == -1) {
      None
    } else {
      Some(quizConfig.rounds(roundIndex).questions(questionNumber))
    }
  }
}

object QuizState {
  implicit val Type: EntityType[QuizState] = EntityType()

  val onlyPossibleId: Long = 1

  def tupled = (this.apply _).tupled
}
