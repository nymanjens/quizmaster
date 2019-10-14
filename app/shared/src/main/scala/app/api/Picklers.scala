package app.api

import app.models.quiz.QuizState
import app.models.quiz.Team
import boopickle.Default._
import hydro.api.StandardPicklers
import hydro.models.Entity

object Picklers extends StandardPicklers {

  override implicit val entityPickler: Pickler[Entity] = compositePickler[Entity]
    .addConcreteType[Team]
    .addConcreteType[QuizState]
}
