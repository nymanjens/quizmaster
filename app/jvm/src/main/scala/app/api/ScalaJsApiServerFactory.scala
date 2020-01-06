package app.api

import app.api.ScalaJsApi._
import app.models.access.JvmEntityAccess
import app.models.quiz.config.QuizConfig
import app.models.user.User
import com.google.inject._
import hydro.api.PicklableDbQuery
import hydro.common.PlayI18n
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.time.Clock
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery

import scala.collection.immutable.Seq

final class ScalaJsApiServerFactory @Inject()(
    implicit clock: Clock,
    entityAccess: JvmEntityAccess,
    i18n: PlayI18n,
    quizConfig: QuizConfig,
    playConfiguration: play.api.Configuration,
) {

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        i18nMessages = i18n.allI18nMessages,
        nextUpdateToken = toUpdateToken(clock.nowInstant),
        quizConfig = quizConfig,
        masterSecret = playConfiguration.get[String]("app.quiz.master-secret"),
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = toUpdateToken(clock.nowInstant)
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.newQuerySync()(entityType).data()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      entityAccess.persistEntityModifications(modifications)
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].data(query)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)
      }
      internal
    }
  }
}
