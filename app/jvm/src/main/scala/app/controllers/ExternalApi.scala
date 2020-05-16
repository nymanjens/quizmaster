package app.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import autowire._
import boopickle.Default._
import java.nio.ByteBuffer

import app.api.Picklers._
import app.api.ScalaJsApi
import app.models.access.JvmEntityAccess
import app.models.modification.EntityTypes
import app.models.quiz.config.QuizConfig
import boopickle.Default.Pickle
import boopickle.Default.Pickler
import boopickle.Default.Unpickle
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.models.modification.EntityModification
import hydro.models.Entity
import hydro.models.modification.EntityType
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._
import scalaj.http.Http
import scalaj.http.HttpOptions

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

final class ExternalApi @Inject()(
    implicit override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    playConfiguration: play.api.Configuration,
    quizConfig: QuizConfig,
) extends AbstractController(components)
    with I18nSupport {

  def importFromOtherServer(secret: String, serverDomain: String) = Action { implicit request =>
    require(secret == quizConfig.masterSecret)

    val allEntitiesResponse = Await.result(
      new HttpPostAutowireClient(serverDomain).apply[ScalaJsApi].getAllEntities(EntityTypes.all).call(),
      atMost = Duration.Inf)

    for (entityType <- EntityTypes.all) {
      entityAccess.persistEntityModifications(
        for (entity <- entityAccess.newQuerySync()(entityType).data())
          yield EntityModification.Remove(entity.id)(entityType)
      )

      entityAccess.persistEntityModifications(
        for (entity <- allEntitiesResponse.entitiesMap(entityType)) yield {
          def internal[E <: Entity](): EntityModification = {
            EntityModification.Add(entity.asInstanceOf[E])(entityType.asInstanceOf[EntityType[E]])
          }

          internal()
        })
    }

    Ok(s"Done importing ${allEntitiesResponse.entitiesMap.values.flatten.size} entities")
  }

  private class HttpPostAutowireClient(serverDomain: String)
      extends autowire.Client[ByteBuffer, Pickler, Pickler] {
    override def doCall(req: Request): Future[ByteBuffer] = {
      Future.successful(
        ByteBuffer.wrap(
          Http(s"http://$serverDomain/scalajsapi/${req.path.last}")
            .postData(Pickle.intoBytes(req.args).array())
            .header("Content-Type", "application/octet-stream")
            .option(HttpOptions.readTimeout(10000))
            .asBytes
            .body
        ))
    }

    override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
    override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
  }
}
