package app.models.quiz

import java.time.Instant

import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class SubmissionEntity(
    teamId: Long,
    roundIndex: Int,
    questionIndex: Int,
    createTime: Instant,
    value: SubmissionValue,
    // If none, there is no information available to make an estimation of correctness
    isCorrectAnswer: Option[Boolean],
    override val idOption: Option[Long] = None,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  override def withId(id: Long) = copy(idOption = Some(id))
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)
}
object SubmissionEntity {
  implicit val Type: EntityType[SubmissionEntity] = EntityType()

  def tupled = (this.apply _).tupled
}
