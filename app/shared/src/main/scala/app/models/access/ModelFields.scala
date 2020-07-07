package app.models.access

import java.time.Instant

import app.common.FixedPointNumber
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.QuizState.TimerState
import app.models.quiz.SubmissionEntity
import app.models.quiz.Team
import hydro.common.CollectionUtils
import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.ScalaUtils
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.models.access.ModelField.IdModelField

import scala.collection.immutable.Seq

object ModelFields {
  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case app.models.quiz.Team.Type             => Team.id.asInstanceOf[ModelField[Long, E]]
    case app.models.quiz.QuizState.Type        => QuizState.id.asInstanceOf[ModelField[Long, E]]
    case app.models.quiz.SubmissionEntity.Type => SubmissionEntity.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Enumeration of all fields **************** //
  object Team {
    private type E = Team

    case object id extends IdModelField[E]
    case object name extends ModelField[String, E]("name", _.name, v => _.copy(name = v))
    case object score extends ModelField[FixedPointNumber, E]("score", _.score, v => _.copy(score = v))
    case object index extends ModelField[Int, E]("index", _.index, v => _.copy(index = v))
  }

  object QuizState {
    private type E = QuizState

    case object id extends IdModelField[E]
    case object roundIndex extends ModelField[Int, E]("roundIndex", _.roundIndex, v => _.copy(roundIndex = v))
    case object questionIndex
        extends ModelField[Int, E]("questionIndex", _.questionIndex, v => _.copy(questionIndex = v))
    case object questionProgressIndex
        extends ModelField[Int, E](
          "questionProgressIndex",
          _.questionProgressIndex,
          v => _.copy(questionProgressIndex = v))
    case object timerState
        extends ModelField[TimerState, E]("timerState", _.timerState, v => _.copy(timerState = v))
    case object submissions
        extends ModelField[Seq[Submission], E]("submissions", _.submissions, v => _.copy(submissions = v))
    case object imageIsEnlarged
        extends ModelField[Boolean, E]("imageIsEnlarged", _.imageIsEnlarged, v => _.copy(imageIsEnlarged = v))
    case object generalQuizSettings
        extends ModelField[GeneralQuizSettings, E](
          "generalQuizSettings",
          _.generalQuizSettings,
          v => _.copy(generalQuizSettings = v))
  }

  object SubmissionEntity {
    private type E = SubmissionEntity

    case object id extends IdModelField[E]
    case object teamId extends ModelField[Long, E]("teamId", _.teamId, v => _.copy(teamId = v))
    case object roundIndex extends ModelField[Int, E]("roundIndex", _.roundIndex, v => _.copy(roundIndex = v))
    case object questionIndex
        extends ModelField[Int, E]("questionIndex", _.questionIndex, v => _.copy(questionIndex = v))
    case object createTime
        extends ModelField[Instant, E]("createTime", _.createTime, v => _.copy(createTime = v))
    case object value extends ModelField[SubmissionValue, E]("value", _.value, v => _.copy(value = v))
    case object isCorrectAnswer
        extends ModelField[Option[Boolean], E](
          "isCorrectAnswer",
          _.isCorrectAnswer,
          v => _.copy(isCorrectAnswer = v))
    case object points extends ModelField[FixedPointNumber, E]("points", _.points, v => _.copy(points = v))
    case object scored extends ModelField[Boolean, E]("scored", _.scored, v => _.copy(scored = v))
  }

  // **************** Field numbers **************** //
  private val fieldToNumberMap: ImmutableBiMap[ModelField.any, Int] =
    CollectionUtils.toBiMapWithStableIntKeys(
      stableNameMapper = field =>
        ScalaUtils.stripRequiredPrefix(field.getClass.getName, prefix = ModelFields.getClass.getName),
      values = Seq(
        Team.id,
        Team.name,
        Team.score,
        Team.index,
        QuizState.id,
        QuizState.roundIndex,
        QuizState.questionIndex,
        QuizState.questionProgressIndex,
        QuizState.timerState,
        QuizState.submissions,
        QuizState.imageIsEnlarged,
        QuizState.generalQuizSettings,
        SubmissionEntity.id,
        SubmissionEntity.teamId,
        SubmissionEntity.roundIndex,
        SubmissionEntity.questionIndex,
        SubmissionEntity.createTime,
        SubmissionEntity.value,
        SubmissionEntity.isCorrectAnswer,
        SubmissionEntity.points,
        SubmissionEntity.scored,
      )
    )
  def toNumber(field: ModelField.any): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField.any = fieldToNumberMap.inverse().get(number)
}
