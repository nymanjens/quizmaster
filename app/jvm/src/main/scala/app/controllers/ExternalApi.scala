package app.controllers

import app.common.RelativePaths.joinPaths
import app.controllers.helpers.media.AlbumParser
import app.controllers.helpers.media.ArtistAssignerFactory
import app.controllers.helpers.media.MediaConfiguration
import app.controllers.helpers.media.MediaScanner
import app.controllers.helpers.media.StoredMediaSyncer
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.media.Album
import app.models.media.Song
import app.models.user.Users
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.common.SerializingTaskQueue
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class ExternalApi @Inject()(
    implicit override val messagesApi: MessagesApi,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    mediaScanner: MediaScanner,
    playConfiguration: play.api.Configuration,
    artistAssignerFactory: ArtistAssignerFactory,
    albumParser: AlbumParser,
    storedMediaSyncer: StoredMediaSyncer,
    mediaConfiguration: MediaConfiguration,
) extends AbstractController(components)
    with I18nSupport {

  // ********** actions ********** //
  def rescanMediaLibrary(applicationSecret: String) = Action { implicit request =>
    validateApplicationSecret(applicationSecret)

    rescanMediaLibraryAsync()

    Ok(s"""
          |OK
          |
          |Parsing has started asynchronously.
          |
          |View progress with the following command:
          |
          |  tail -f /tmp/musik-logs
       """.stripMargin.trim)
  }

  // ********** private helper methods ********** //
  private val rescanMediaLibraryAsyncQueue = SerializingTaskQueue.withAtMostSingleQueuedTask()
  private def rescanMediaLibraryAsync(): Future[_] =
    rescanMediaLibraryAsyncQueue.schedule {
      Future {
        try {
          val oldRelativePaths = {
            val albumIdToRelativePath =
              entityAccess.newQuerySync[Album]().data().map(album => album.id -> album.relativePath).toMap
            entityAccess
              .newQuerySync[Song]()
              .data()
              .map(song => joinPaths(albumIdToRelativePath(song.albumId), song.filename))
          }
          println(s"  Found ${oldRelativePaths.size} existing songs.")

          val addedAndRemovedMedia =
            mediaScanner
              .scanAddedAndRemovedMedia(
                folderToScan = mediaConfiguration.mediaFolder,
                oldRelativePaths = oldRelativePaths.toSet)
          println(
            s"  Found ${oldRelativePaths.size} existing songs, " +
              s"${addedAndRemovedMedia.added.size} added files " +
              s"and ${addedAndRemovedMedia.removedRelativePaths.size} removed files.")

          val artistAssigner = artistAssignerFactory.fromDbAndMediaFiles(addedAndRemovedMedia.added)
          val parsedAlbums = albumParser.parse(addedAndRemovedMedia.added, artistAssigner)
          println(s"  Parsed ${parsedAlbums.size} albums.")

          storedMediaSyncer.addEntitiesFromParsedAlbums(parsedAlbums)
          storedMediaSyncer.removeEntitiesFromRelativeSongPaths(addedAndRemovedMedia.removedRelativePaths)
          println(s"  Done! Added ${parsedAlbums.size} albums.")
        } catch {
          case throwable: Throwable =>
            println(s"  Caught exception: $throwable")
            throwable.printStackTrace()
        }
      }
    }

  private def validateApplicationSecret(applicationSecret: String): Unit = {
    val realApplicationSecret: String = playConfiguration.get[String]("play.http.secret.key")
    require(
      applicationSecret == realApplicationSecret,
      s"Invalid application secret. Found '$applicationSecret' but should be '$realApplicationSecret'")
  }
}
