package app.controllers.helpers

import java.nio.ByteBuffer
import app.api.Picklers._
import app.api.ScalaJsApi.TeamOrQuizStateUpdate
import app.api.ScalaJsApiServerFactory
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.user.User.onlyUser
import boopickle.Default._
import com.google.inject.Inject
import hydro.api.PicklableDbQuery
import hydro.controllers.InternalApi.ScalaJsApiCaller
import hydro.models.Entity
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import play.api.mvc.RequestHeader
import play.mvc.Http

import scala.collection.immutable.Seq

final class ScalaJsApiCallerImpl @Inject() (implicit scalaJsApiServerFactory: ScalaJsApiServerFactory)
    extends ScalaJsApiCaller {

  override def apply(path: String, argsMap: Map[String, ByteBuffer])(implicit request: RequestHeader): ByteBuffer = {
    val scalaJsApiServer = scalaJsApiServerFactory.create(request)

    path match {
      case "getInitialData" =>
        Pickle.intoBytes(scalaJsApiServer.getInitialData())
      case "getAllEntities" =>
        val types = Unpickle[Seq[EntityType.any]].fromBytes(argsMap("types"))
        Pickle.intoBytes(scalaJsApiServer.getAllEntities(types))
      case "persistEntityModifications" =>
        val modifications = Unpickle[Seq[EntityModification]].fromBytes(argsMap("modifications"))
        Pickle.intoBytes(scalaJsApiServer.persistEntityModifications(modifications))
      case "executeDataQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes[Seq[Entity]](scalaJsApiServer.executeDataQuery(dbQuery))
      case "executeCountQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes(scalaJsApiServer.executeCountQuery(dbQuery))
      case "doTeamOrQuizStateUpdate" =>
        val teamOrQuizStateUpdate =
          Unpickle[TeamOrQuizStateUpdate].fromBytes(argsMap("teamOrQuizStateUpdate"))
        Pickle.intoBytes(scalaJsApiServer.doTeamOrQuizStateUpdate(teamOrQuizStateUpdate))
    }
  }
}
