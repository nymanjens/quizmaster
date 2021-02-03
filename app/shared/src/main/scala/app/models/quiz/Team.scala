package app.models.quiz

import app.common.FixedPointNumber
import hydro.models.Entity
import hydro.models.UpdatableEntity
import hydro.models.UpdatableEntity.LastUpdateTime
import hydro.models.modification.EntityType

case class Team(
    name: String,
    score: FixedPointNumber,
    /** Index in teams list. */
    index: Int,
    override val idOption: Option[Long] = None,
    override val lastUpdateTime: LastUpdateTime = LastUpdateTime.neverUpdated,
) extends UpdatableEntity {

  override def withId(id: Long) = copy(idOption = Some(id))
  override def withLastUpdateTime(time: LastUpdateTime): Entity = copy(lastUpdateTime = time)
}

object Team {
  implicit val Type: EntityType[Team] = EntityType()

  def tupled = (this.apply _).tupled

  def areEquivalentTeamNames(name1: String, name2: String): Boolean = {
    def normalizeTextForComparison(s: String): String = {
      s.replace(" ", "").replace(".", "").replace("-", "").toLowerCase
    }
    normalizeTextForComparison(name1) == normalizeTextForComparison(name2)
  }

  def ordering(implicit quizState: QuizState): Ordering[Team] = {
    Ordering.by(team =>
      if (quizState.generalQuizSettings.sortTeamsByScore) (team.score.negate, team.index)
      else (FixedPointNumber(0), team.index)
    )
  }
}
