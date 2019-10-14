package app.flux.stores

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.flux.stores.media.helpers.ComplexQueryFilterFactory
import app.flux.stores.media.AlbumDetailStoreFactory
import app.flux.stores.media.ArtistDetailStoreFactory
import app.flux.stores.media.ComplexQueryStoreFactory
import app.flux.stores.media.PlaylistStore
import app.flux.stores.media.PlayStatusStore
import app.flux.stores.media.SongAnnotationStore
import app.flux.stores.media.JsSongStoreFactory
import app.flux.stores.media.JsAlbumStoreFactory
import app.models.user.User
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.LocalDatabaseHasBeenLoadedStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.flux.stores.UserStore
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.JsEntityAccess

final class Module(
    implicit i18n: I18n,
    user: User,
    entityAccess: JsEntityAccess,
    dispatcher: Dispatcher,
    clock: Clock,
    scalaJsApiClient: ScalaJsApiClient,
    hydroPushSocketClientFactory: HydroPushSocketClientFactory,
    getInitialDataResponse: GetInitialDataResponse,
) {

  implicit val userStore = new UserStore
  implicit val globalMessagesStore = new GlobalMessagesStore
  implicit val pageLoadingStateStore = new PageLoadingStateStore
  implicit val pendingModificationsStore = new PendingModificationsStore
  implicit val applicationIsOnlineStore = new ApplicationIsOnlineStore
  implicit val localDatabaseHasBeenLoadedStore = new LocalDatabaseHasBeenLoadedStore

  implicit private val complexQueryFilterFactory = new ComplexQueryFilterFactory

  implicit val playlistStore = new PlaylistStore
  implicit val playStatusStore = PlayStatusStore()
  implicit val jsSongStoreFactory = new JsSongStoreFactory
  implicit val jsAlbumStoreFactory = new JsAlbumStoreFactory
  implicit val songAnnotationStore = new SongAnnotationStore
  implicit val albumDetailStoreFactory = new AlbumDetailStoreFactory
  implicit val artistDetailStoreFactory = new ArtistDetailStoreFactory
  implicit val complexQueryStoreFactory = new ComplexQueryStoreFactory
}
