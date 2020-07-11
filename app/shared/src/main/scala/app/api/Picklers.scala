package app.api

import app.common.FixedPointNumber
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.SubmissionEntity
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import boopickle.Default._
import boopickle.DefaultBasic.PicklerGenerator
import hydro.api.StandardPicklers
import hydro.models.Entity

object Picklers extends StandardPicklers {

  implicit object FixedPointNumberPickler extends Pickler[FixedPointNumber] {
    override def pickle(obj: FixedPointNumber)(implicit state: PickleState): Unit = state.pickle(obj.toDouble)
    override def unpickle(implicit state: UnpickleState): FixedPointNumber =
      FixedPointNumber(state.unpickle[Double])
  }

  implicit val questionPickler: Pickler[Question] = compositePickler[Question]
    .addConcreteType[Question.Standard]
    .addConcreteType[Question.DoubleQ]
    .addConcreteType[Question.OrderItems]

  implicit val answerBulletTypePickler: Pickler[AnswerBulletType] = compositePickler[AnswerBulletType]
    .addConcreteType[AnswerBulletType.Arrows.type]
    .addConcreteType[AnswerBulletType.Characters.type]

  implicit val submissionValuePickler: Pickler[SubmissionValue] = compositePickler[SubmissionValue]
    .addConcreteType[SubmissionValue.PressedTheOneButton.type]
    .addConcreteType[SubmissionValue.MultipleChoiceAnswer]
    .addConcreteType[SubmissionValue.FreeTextAnswer]

  override implicit val entityPickler: Pickler[Entity] = compositePickler[Entity]
    .addConcreteType[Team]
    .addConcreteType[QuizState]
    .addConcreteType[SubmissionEntity]

}
