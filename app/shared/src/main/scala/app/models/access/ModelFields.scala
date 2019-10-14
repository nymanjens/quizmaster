package app.models.access

import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.OrderToken
import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlayStatus
import app.models.media.PlaylistEntry
import app.models.media.Song
import app.models.media.SongAnnotation
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.common.CollectionUtils
import hydro.common.ScalaUtils
import hydro.models.Entity
import hydro.models.access.ModelField
import hydro.models.access.ModelField.IdModelField

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration

object ModelFields {
  // **************** Methods **************** //
  def id[E <: Entity](implicit entityType: EntityType[E]): ModelField[Long, E] = entityType match {
    case app.models.user.User.Type            => User.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Song.Type           => Song.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Album.Type          => Album.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.Artist.Type         => Artist.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.PlaylistEntry.Type  => PlaylistEntry.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.PlayStatus.Type     => PlayStatus.id.asInstanceOf[ModelField[Long, E]]
    case app.models.media.SongAnnotation.Type => SongAnnotation.id.asInstanceOf[ModelField[Long, E]]
  }

  // **************** Enumeration of all fields **************** //
  object User {
    private type E = User

    case object id extends IdModelField[E]
    case object loginName extends ModelField[String, E]("loginName", _.loginName, v => _.copy(loginName = v))
    case object passwordHash
        extends ModelField[String, E]("passwordHash", _.passwordHash, v => _.copy(passwordHash = v))
    case object name extends ModelField[String, E]("name", _.name, v => _.copy(name = v))
    case object isAdmin extends ModelField[Boolean, E]("isAdmin", _.isAdmin, v => _.copy(isAdmin = v))
  }

  object Song {
    private type E = Song

    case object id extends IdModelField[E]
    case object filename extends ModelField[String, E]("filename", _.filename, v => _.copy(filename = v))
    case object title extends ModelField[String, E]("title", _.title, v => _.copy(title = v))
    case object albumId extends ModelField[Long, E]("albumId", _.albumId, v => _.copy(albumId = v))
    case object artistId
        extends ModelField[Option[Long], E]("artistId", _.artistId, v => _.copy(artistId = v))
    case object trackNumber
        extends ModelField[Int, E]("trackNumber", _.trackNumber, v => _.copy(trackNumber = v))
    case object duration
        extends ModelField[Option[FiniteDuration], E]("duration", _.duration, v => _.copy(duration = v))
    case object disc extends ModelField[Int, E]("disc", _.disc, v => _.copy(disc = v))
  }

  object Album {
    private type E = Album

    case object id extends IdModelField[E]
    case object relativePath
        extends ModelField[String, E]("relativePath", _.relativePath, v => _.copy(relativePath = v))
    case object title extends ModelField[String, E]("title", _.title, v => _.copy(title = v))
    case object artistId
        extends ModelField[Option[Long], E]("artistId", _.artistId, v => _.copy(artistId = v))
    case object year extends ModelField[Option[Int], E]("year", _.year, v => _.copy(year = v))
  }

  object Artist {
    private type E = Artist

    case object id extends IdModelField[E]
    case object name extends ModelField[String, E]("name", _.name, v => _.copy(name = v))
  }

  object PlaylistEntry {
    private type E = PlaylistEntry

    case object id extends IdModelField[E]
    case object songId extends ModelField[Long, E]("songId", _.songId, v => _.copy(songId = v))
    case object orderToken
        extends ModelField[OrderToken, E]("orderToken", _.orderToken, v => _.copy(orderToken = v))
    case object userId extends ModelField[Long, E]("userId", _.userId, v => _.copy(userId = v))
    case object albumId extends ModelField[Long, E]("albumId", _.albumId, v => _.copy(albumId = v))
  }

  object PlayStatus {
    private type E = PlayStatus

    case object id extends IdModelField[E]
    case object currentPlaylistEntryId
        extends ModelField[Long, E](
          "currentPlaylistEntryId",
          _.currentPlaylistEntryId,
          v => _.copy(currentPlaylistEntryId = v))
    case object hasStarted
        extends ModelField[Boolean, E]("hasStarted", _.hasStarted, v => _.copy(hasStarted = v))
    case object stopAfterCurrentSong
        extends ModelField[Boolean, E](
          "stopAfterCurrentSong",
          _.stopAfterCurrentSong,
          v => _.copy(stopAfterCurrentSong = v))
    case object userId extends ModelField[Long, E]("userId", _.userId, v => _.copy(userId = v))
  }

  object SongAnnotation {
    private type E = SongAnnotation

    case object id extends IdModelField[E]
    case object songId extends ModelField[Long, E]("songId", _.songId, v => _.copy(songId = v))
    case object userId extends ModelField[Long, E]("userId", _.userId, v => _.copy(userId = v))
    case object playCount extends ModelField[Int, E]("playCount", _.playCount, v => _.copy(playCount = v))
    case object liked extends ModelField[Boolean, E]("liked", _.liked, v => _.copy(liked = v))
  }

  // **************** Field numbers **************** //
  private val fieldToNumberMap: ImmutableBiMap[ModelField.any, Int] =
    CollectionUtils.toBiMapWithStableIntKeys(
      stableNameMapper = field =>
        ScalaUtils.stripRequiredPrefix(field.getClass.getName, prefix = ModelFields.getClass.getName),
      values = Seq(
        User.id,
        User.loginName,
        User.passwordHash,
        User.name,
        User.isAdmin,
        Song.id,
        Song.filename,
        Song.title,
        Song.albumId,
        Song.artistId,
        Song.trackNumber,
        Song.duration,
        Song.disc,
        Album.id,
        Album.relativePath,
        Album.title,
        Album.artistId,
        Album.year,
        Artist.id,
        Artist.name,
        PlaylistEntry.id,
        PlaylistEntry.songId,
        PlaylistEntry.orderToken,
        PlaylistEntry.userId,
        PlayStatus.id,
        PlayStatus.currentPlaylistEntryId,
        PlayStatus.hasStarted,
        PlayStatus.stopAfterCurrentSong,
        PlayStatus.userId,
        SongAnnotation.id,
        SongAnnotation.songId,
        SongAnnotation.userId,
        SongAnnotation.playCount,
        SongAnnotation.liked,
      )
    )
  def toNumber(field: ModelField.any): Int = fieldToNumberMap.get(field)
  def fromNumber(number: Int): ModelField.any = fieldToNumberMap.inverse().get(number)
}
