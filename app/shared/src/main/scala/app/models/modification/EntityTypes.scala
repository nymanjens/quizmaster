package app.models.modification

import app.models.quiz.QuizState
import app.models.quiz.Team
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

object EntityTypes {

  private[models] val fullySyncedLocally: Seq[EntityType.any] = Seq()

  val all: Seq[EntityType.any] = fullySyncedLocally ++ Seq(Team.Type, QuizState.Type)
}
