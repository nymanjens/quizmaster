package app.flux

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.models.user.User.onlyUser
import hydro.flux.action.Module
import hydro.flux.router.Page
import japgolly.scalajs.react.extra.router.Router

final class ClientAppModule(
    implicit getInitialDataResponse: GetInitialDataResponse,
    scalaJsApiClient: ScalaJsApiClient,
) {

  // Create and unpack common modules
  private val commonTimeModule = new hydro.common.time.Module
  implicit private val clock = commonTimeModule.clock
  private val commonModule = new hydro.common.Module
  implicit private val i18n = commonModule.i18n

  // Create and unpack Models Access module
  val modelsAccessModule = new app.models.access.Module
  implicit val entityAccess = modelsAccessModule.entityAccess
  implicit val hydroPushSocketClientFactory = modelsAccessModule.hydroPushSocketClientFactory

  // Create and unpack Flux action module
  private val fluxActionModule = new Module
  implicit private val dispatcher = fluxActionModule.dispatcher

  // Create and unpack Flux store module
  private val fluxStoresModule = new app.flux.stores.Module
  implicit private val globalMessagesStore = fluxStoresModule.globalMessagesStore
  implicit private val pageLoadingStateStore = fluxStoresModule.pageLoadingStateStore
  implicit private val pendingModificationsStore = fluxStoresModule.pendingModificationsStore
  implicit private val applicationIsOnlineStore = fluxStoresModule.applicationIsOnlineStore

  // Create other Flux modules
  implicit private val reactAppModule = new app.flux.react.app.Module
  implicit private val routerModule = new app.flux.router.Module

  val router: Router[Page] = routerModule.router
}
