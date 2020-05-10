package app.models.modification

import app.models.quiz.QuizState
import app.models.quiz.SubmissionEntity
import app.models.quiz.Team
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

object EntityTypes {

  val fullySyncedLocally: Seq[EntityType.any] = Seq(Team.Type, QuizState.Type)

  val all: Seq[EntityType.any] = fullySyncedLocally ++ Seq(SubmissionEntity.Type)
}
