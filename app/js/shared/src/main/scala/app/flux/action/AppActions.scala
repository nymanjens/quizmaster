package app.flux.action

import app.flux.action.AppActions.AddSongsToPlaylist.Placement
import app.models.media.Song
import hydro.flux.action.Action

import scala.collection.immutable.Seq

object AppActions {

  // **************** Media-related actions **************** //
  case class AddSongsToPlaylist(songs: Seq[Song], placement: Placement) extends Action
  object AddSongsToPlaylist {
    sealed trait Placement
    object Placement {
      object AfterCurrentSong extends Placement
      object AtEnd extends Placement
    }
  }

  case class RemoveEntriesFromPlaylist(playlistEntryIds: Seq[Long]) extends Action

  object RemoveAllPlayedEntriesFromPlaylist extends Action

  // SongAnnotation actions
  case class IncrementSongPlayCount(songId: Long) extends Action
  case class UpdateSongLiked(songId: Long, liked: Boolean) extends Action
}
