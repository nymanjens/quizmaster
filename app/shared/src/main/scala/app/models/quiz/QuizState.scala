package app.models.quiz

import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class QuizState(
    partNumber: Int,
    questionNumber: Int,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  override val idOption: Option[Long] = Some(1)
  override def withId(id: Long) = {
    require(id == 1)
    this
  }
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)
}

object QuizState {
  implicit val Type: EntityType[QuizState] = EntityType()

  def tupled = (this.apply _).tupled
}
