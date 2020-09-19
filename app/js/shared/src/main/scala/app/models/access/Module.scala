package app.models.access

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.modification.EntityTypes
import app.models.user.User
import hydro.common.time.Clock
import hydro.models.access.EntitySyncLogic
import hydro.models.access.HybridRemoteDatabaseProxy
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.InMemoryLocalDatabase
import hydro.models.access.JsEntityAccess
import hydro.models.access.JsEntityAccessImpl
import hydro.models.access.LocalDatabase

import scala.concurrent.Future
import scala.concurrent.Promise

final class Module(implicit
    user: User,
    clock: Clock,
    scalaJsApiClient: ScalaJsApiClient,
    getInitialDataResponse: GetInitialDataResponse,
) {

  implicit private val entitySyncLogic = new EntitySyncLogic.FullySynced(EntityTypes.fullySyncedLocally)

  implicit val hydroPushSocketClientFactory: HydroPushSocketClientFactory =
    new HydroPushSocketClientFactory()

  implicit val entityAccess: JsEntityAccess = {
    implicit val remoteDatabaseProxy =
      HybridRemoteDatabaseProxy.create(Future.successful(new InMemoryLocalDatabase))
    val entityAccess = new JsEntityAccessImpl()

    entityAccess.startCheckingForModifiedEntityUpdates()

    entityAccess
  }
}
