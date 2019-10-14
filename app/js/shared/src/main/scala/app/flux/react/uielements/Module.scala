package app.flux.react.uielements

import app.flux.react.uielements.media.EnqueueableAlbumDiv
import app.flux.react.uielements.media.ArtistDiv
import hydro.common.I18n
import app.flux.react.uielements.media.MusicPlayerDiv
import app.flux.react.uielements.media.PlaylistEntryDiv
import app.flux.react.uielements.media.AlbumPlaylistEntryDiv
import app.flux.react.uielements.media.EnqueueableSongDiv
import app.flux.stores.media.AlbumDetailStoreFactory
import app.flux.stores.media.ArtistDetailStoreFactory
import app.flux.stores.media.JsSongStoreFactory
import app.flux.stores.media.JsAlbumStoreFactory
import app.flux.stores.media.PlayStatusStore
import hydro.flux.action.Dispatcher
import hydro.models.access.EntityAccess

final class Module(
    implicit i18n: I18n,
    entityAccess: EntityAccess,
    dispatcher: Dispatcher,
    playStatusStore: PlayStatusStore,
    artistDetailStoreFactory: ArtistDetailStoreFactory,
    albumDetailStoreFactory: AlbumDetailStoreFactory,
    jsSongStoreFactory: JsSongStoreFactory,
    jsAlbumStoreFactory: JsAlbumStoreFactory,
) {

  implicit lazy val enqueueableSongDiv = new EnqueueableSongDiv
  implicit lazy val musicPlayerDiv = new MusicPlayerDiv
  implicit lazy val artistDiv = new ArtistDiv
  implicit lazy val enqueueableAlbumDiv = new EnqueueableAlbumDiv
  implicit lazy val playlistEntryDiv = new PlaylistEntryDiv
  implicit lazy val albumPlaylistEntryDiv = new AlbumPlaylistEntryDiv
}
