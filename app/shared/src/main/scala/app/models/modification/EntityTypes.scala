package app.models.modification

import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.media.SongAnnotation
import app.models.user.User
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

object EntityTypes {

  private[models] val fullySyncedLocally: Seq[EntityType.any] =
    Seq(User.Type, PlaylistEntry.Type, PlayStatus.Type)
  private val partiallySyncedLocally: Seq[EntityType.any] =
    Seq(Song.Type, Album.Type, Artist.Type, SongAnnotation.Type)

  val all: Seq[EntityType.any] = fullySyncedLocally ++ partiallySyncedLocally
}
