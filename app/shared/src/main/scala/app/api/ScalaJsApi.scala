package app.api

import java.time.Duration

import app.api.ScalaJsApi._
import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.Team
import hydro.api.PicklableDbQuery
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq

/** API for communication between client and server (clients calls server). */
trait ScalaJsApi {

  /** Returns most information needed to render the first page. */
  def getInitialData(): GetInitialDataResponse

  /** Returns a map, mapping the entity type to a sequence of all entities of that type. */
  def getAllEntities(types: Seq[EntityType.any]): GetAllEntitiesResponse

  /** Stores the given entity modifications. */
  def persistEntityModifications(modifications: Seq[EntityModification]): Unit

  def executeDataQuery(dbQuery: PicklableDbQuery): Seq[Entity]

  def executeCountQuery(dbQuery: PicklableDbQuery): Int

  def doTeamOrQuizStateUpdate(teamOrQuizStateUpdate: TeamOrQuizStateUpdate): Unit
}

object ScalaJsApi {
  type UpdateToken = String

  /**
    * @param i18nMessages Maps key to the message with placeholders.
    * @param nextUpdateToken An update token for all changes since this call
    */
  case class GetInitialDataResponse(
      i18nMessages: Map[String, String],
      nextUpdateToken: UpdateToken,
      quizConfig: QuizConfig,
  )

  case class GetAllEntitiesResponse(
      entitiesMap: Map[EntityType.any, Seq[Entity]],
      nextUpdateToken: UpdateToken,
  ) {
    def entityTypes: Iterable[EntityType.any] = entitiesMap.keys
    def entities[E <: Entity](entityType: EntityType[E]): Seq[E] = {
      entitiesMap(entityType).asInstanceOf[Seq[E]]
    }
  }

  sealed trait HydroPushSocketPacket
  object HydroPushSocketPacket {
    case class EntityModificationsWithToken(
        modifications: Seq[EntityModification],
        nextUpdateToken: UpdateToken,
    ) extends HydroPushSocketPacket
    object Heartbeat extends HydroPushSocketPacket
    case class VersionCheck(versionString: String) extends HydroPushSocketPacket
  }

  sealed trait TeamOrQuizStateUpdate
  object TeamOrQuizStateUpdate {
    case class ReplaceAllEntitiesByImportString(importString: String) extends TeamOrQuizStateUpdate
    case class UpdateName(teamId: Long, newName: String) extends TeamOrQuizStateUpdate
    case class UpdateScore(teamId: Long, scoreDiff: FixedPointNumber) extends TeamOrQuizStateUpdate
    case class DeleteTeam(teamId: Long) extends TeamOrQuizStateUpdate
    case class GoToPreviousStep() extends TeamOrQuizStateUpdate
    case class GoToNextStep() extends TeamOrQuizStateUpdate
    case class GoToPreviousQuestion() extends TeamOrQuizStateUpdate

    /**
      * Go to the start of the current question if it's not already there or the start of the previous question
      * otherwise
      */
    case class GoToNextQuestion() extends TeamOrQuizStateUpdate

    /**
      * Go to the start of the current round if it's not already there or the start of the previous round
      * otherwise
      */
    case class GoToPreviousRound() extends TeamOrQuizStateUpdate
    case class GoToNextRound() extends TeamOrQuizStateUpdate
    case class ResetCurrentQuestion() extends TeamOrQuizStateUpdate
    case class ToggleImageIsEnlarged() extends TeamOrQuizStateUpdate
    case class SetShowAnswers(showAnswers: Boolean) extends TeamOrQuizStateUpdate
    case class SetAnswerBulletType(answerBulletType: AnswerBulletType) extends TeamOrQuizStateUpdate
    case class ToggleTimerPaused(timerRunningValue: Option[Boolean] = None) extends TeamOrQuizStateUpdate
    case class AddTimeToTimer(duration: Duration) extends TeamOrQuizStateUpdate

    /** Start from the beginning if audio/video is playing. */
    case class RestartMedia() extends TeamOrQuizStateUpdate

    case class AddSubmission(teamId: Long, submissionValue: SubmissionValue) extends TeamOrQuizStateUpdate
    case class SetSubmissionCorrectness(submissionId: Long, isCorrectAnswer: Boolean)
        extends TeamOrQuizStateUpdate

    /** If the given points are non-zero, the correctness is also updated based on the sign of the given points. */
    case class SetSubmissionPoints(submissionId: Long, points: FixedPointNumber) extends TeamOrQuizStateUpdate
  }
}
