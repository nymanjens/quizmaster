package app.api

import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.Team
import boopickle.Default._
import hydro.api.StandardPicklers
import hydro.models.Entity

object Picklers extends StandardPicklers {

  implicit val answerBulletTypePickler: Pickler[AnswerBulletType] = compositePickler[AnswerBulletType]
    .addConcreteType[AnswerBulletType.Arrows.type]
    .addConcreteType[AnswerBulletType.Characters.type]

  override implicit val entityPickler: Pickler[Entity] = compositePickler[Entity]
    .addConcreteType[Team]
    .addConcreteType[QuizState]
}
