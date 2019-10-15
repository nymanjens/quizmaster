package app.flux.stores.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class TeamsAndQuizStateStore(
    implicit entityAccess: JsEntityAccess,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock,
) extends AsyncEntityDerivedStateStore[State] {

  override protected def calculateState(): Future[State] = async {
    State(
      teams = await(
        entityAccess
          .newQuery[Team]()
          .sort(DbQuery.Sorting.ascBy(ModelFields.Team.createTimeMillisSinceEpoch))
          .data()),
      maybeQuizState = await(
        entityAccess.newQuery[QuizState]().findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)),
    )
  }

  override protected def modificationImpactsState(
      entityModification: EntityModification,
      state: State,
  ): Boolean = true

  def stateOrEmpty: State = state getOrElse State(teams = Seq(), maybeQuizState = None)
}

object TeamsAndQuizStateStore {
  case class State(
      teams: Seq[Team],
      maybeQuizState: Option[QuizState],
  )
}
