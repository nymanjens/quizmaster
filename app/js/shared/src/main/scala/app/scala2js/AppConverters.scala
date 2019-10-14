package app.scala2js

import hydro.models.modification.EntityType
import app.models.access.ModelFields
import app.models.media._
import app.models.modification.EntityTypes
import app.models.user.User
import hydro.models.Entity
import hydro.scala2js.Scala2Js.Converter
import hydro.scala2js.Scala2Js.MapConverter
import hydro.scala2js.StandardConverters
import hydro.scala2js.StandardConverters.EntityConverter

import scala.collection.immutable.Seq

object AppConverters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: MapConverter[_ <: Entity] = entityType match {
      case User.Type           => UserConverter
      case Song.Type           => SongConverter
      case Album.Type          => AlbumConverter
      case Artist.Type         => ArtistConverter
      case PlaylistEntry.Type  => PlaylistEntryConverter
      case PlayStatus.Type     => PlayStatusConverter
      case SongAnnotation.Type => SongAnnotationConverter
    }
    converter.asInstanceOf[MapConverter[E]]
  }

  // **************** Entity converters **************** //
  implicit val UserConverter: EntityConverter[User] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.User.loginName,
      ModelFields.User.passwordHash,
      ModelFields.User.name,
      ModelFields.User.isAdmin,
    ),
    toScalaWithoutId = dict =>
      User(
        loginName = dict.getRequired(ModelFields.User.loginName),
        passwordHash = dict.getRequired(ModelFields.User.passwordHash),
        name = dict.getRequired(ModelFields.User.name),
        isAdmin = dict.getRequired(ModelFields.User.isAdmin)
    )
  )

  implicit val SongConverter: EntityConverter[Song] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Song.filename,
      ModelFields.Song.title,
      ModelFields.Song.albumId,
      ModelFields.Song.artistId,
      ModelFields.Song.trackNumber,
      ModelFields.Song.duration,
      ModelFields.Song.disc,
    ),
    toScalaWithoutId = dict =>
      Song(
        filename = dict.getRequired(ModelFields.Song.filename),
        title = dict.getRequired(ModelFields.Song.title),
        albumId = dict.getRequired(ModelFields.Song.albumId),
        artistId = dict.getRequired(ModelFields.Song.artistId),
        trackNumber = dict.getRequired(ModelFields.Song.trackNumber),
        duration = dict.getRequired(ModelFields.Song.duration),
        disc = dict.getRequired(ModelFields.Song.disc)
    )
  )

  implicit val SongAnnotationConverter: EntityConverter[SongAnnotation] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.SongAnnotation.songId,
      ModelFields.SongAnnotation.userId,
      ModelFields.SongAnnotation.playCount,
      ModelFields.SongAnnotation.liked,
    ),
    toScalaWithoutId = dict =>
      SongAnnotation(
        songId = dict.getRequired(ModelFields.SongAnnotation.songId),
        userId = dict.getRequired(ModelFields.SongAnnotation.userId),
        playCount = dict.getRequired(ModelFields.SongAnnotation.playCount),
        liked = dict.getRequired(ModelFields.SongAnnotation.liked),
    )
  )
  implicit val AlbumConverter: EntityConverter[Album] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Album.relativePath,
      ModelFields.Album.title,
      ModelFields.Album.artistId,
      ModelFields.Album.year,
    ),
    toScalaWithoutId = dict =>
      Album(
        relativePath = dict.getRequired(ModelFields.Album.relativePath),
        title = dict.getRequired(ModelFields.Album.title),
        artistId = dict.getRequired(ModelFields.Album.artistId),
        year = dict.getRequired(ModelFields.Album.year)
    )
  )
  implicit val ArtistConverter: EntityConverter[Artist] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.Artist.name,
    ),
    toScalaWithoutId = dict => Artist(name = dict.getRequired(ModelFields.Artist.name))
  )
  implicit val PlaylistEntryConverter: EntityConverter[PlaylistEntry] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.PlaylistEntry.songId,
      ModelFields.PlaylistEntry.orderToken,
      ModelFields.PlaylistEntry.userId,
      ModelFields.PlaylistEntry.albumId,
    ),
    toScalaWithoutId = dict =>
      PlaylistEntry(
        songId = dict.getRequired(ModelFields.PlaylistEntry.songId),
        orderToken = dict.getRequired(ModelFields.PlaylistEntry.orderToken),
        userId = dict.getRequired(ModelFields.PlaylistEntry.userId),
        albumId = dict.getRequired(ModelFields.PlaylistEntry.albumId),
    )
  )
  implicit val PlayStatusConverter: EntityConverter[PlayStatus] = new EntityConverter(
    allFieldsWithoutId = Seq(
      ModelFields.PlayStatus.currentPlaylistEntryId,
      ModelFields.PlayStatus.hasStarted,
      ModelFields.PlayStatus.stopAfterCurrentSong,
      ModelFields.PlayStatus.userId,
    ),
    toScalaWithoutId = dict =>
      PlayStatus(
        currentPlaylistEntryId = dict.getRequired(ModelFields.PlayStatus.currentPlaylistEntryId),
        hasStarted = dict.getRequired(ModelFields.PlayStatus.hasStarted),
        stopAfterCurrentSong = dict.getRequired(ModelFields.PlayStatus.stopAfterCurrentSong),
        userId = dict.getRequired(ModelFields.PlayStatus.userId)
    )
  )
}
