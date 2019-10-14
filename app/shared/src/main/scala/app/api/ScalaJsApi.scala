package app.api

import app.api.ScalaJsApi._
import app.models.quiz.config.QuizConfig
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
}
