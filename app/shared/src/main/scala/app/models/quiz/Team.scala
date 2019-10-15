package app.models.quiz

import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class Team(
    name: String,
    score: Int,
    createTimeMillisSinceEpoch: Long,
    override val idOption: Option[Long] = None,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  override def withId(id: Long) = copy(idOption = Some(id))
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)
}

object Team {
  implicit val Type: EntityType[Team] = EntityType()

  def tupled = (this.apply _).tupled
}
