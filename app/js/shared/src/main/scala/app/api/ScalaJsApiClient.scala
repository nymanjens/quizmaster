package app.api

import java.nio.ByteBuffer

import app.api.ScalaJsApi._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import autowire._
import boopickle.Default._
import app.api.Picklers._
import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.api.PicklableDbQuery
import hydro.api.ScalaJsApiRequest
import hydro.common.JsLoggingUtils.logExceptions
import hydro.models.Entity
import hydro.models.access.DbQuery
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray._

trait ScalaJsApiClient {

  def getInitialData(): Future[GetInitialDataResponse]
  def getAllEntities(types: Seq[EntityType.any]): Future[GetAllEntitiesResponse]
  def persistEntityModifications(modifications: Seq[EntityModification]): Future[Unit]
  def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]): Future[Seq[E]]
  def executeCountQuery(dbQuery: DbQuery[_ <: Entity]): Future[Int]
  def doTeamOrQuizStateUpdate(teamOrQuizStateUpdate: TeamOrQuizStateUpdate): Future[Unit]
}

object ScalaJsApiClient {

  final class Impl extends ScalaJsApiClient {

    override def getInitialData() = {
      HttpGetAutowireClient[ScalaJsApi].getInitialData().call()
    }

    override def getAllEntities(types: Seq[EntityType.any]) = {
      HttpPostAutowireClient[ScalaJsApi].getAllEntities(types).call()
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]) = {
      HttpPostAutowireClient[ScalaJsApi].persistEntityModifications(modifications).call()
    }

    override def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      HttpPostAutowireClient[ScalaJsApi]
        .executeDataQuery(picklableDbQuery)
        .call()
        .map(_.asInstanceOf[Seq[E]])
    }

    override def executeCountQuery(dbQuery: DbQuery[_ <: Entity]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      HttpPostAutowireClient[ScalaJsApi].executeCountQuery(picklableDbQuery).call()
    }

    override def doTeamOrQuizStateUpdate(teamOrQuizStateUpdate: TeamOrQuizStateUpdate) = {
      HttpPostAutowireClient[ScalaJsApi]
        .doTeamOrQuizStateUpdate(teamOrQuizStateUpdate)
        .call()
    }

    private object HttpPostAutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      override def doCall(req: Request): Future[ByteBuffer] = {
        dom.ext.Ajax
          .post(
            url = "/scalajsapi/" + req.path.last,
            data = Pickle.intoBytes(req.args),
            responseType = "arraybuffer",
            headers = Map("Content-Type" -> "application/octet-stream")
          )
          .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }

    private object HttpGetAutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      override def doCall(req: Request): Future[ByteBuffer] = {
        require(req.args.isEmpty)
        dom.ext.Ajax
          .get(
            url = "/scalajsapi/" + req.path.last,
            responseType = "arraybuffer",
            headers = Map("Content-Type" -> "application/octet-stream")
          )
          .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }
  }
}
