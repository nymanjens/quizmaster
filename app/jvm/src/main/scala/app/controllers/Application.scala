package app.controllers

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import app.api.ScalaJsApiServerFactory
import app.common.RelativePaths
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
import com.google.common.base.Throwables
import com.google.common.io.BaseEncoding
import com.google.inject.Inject
import hydro.common.publisher.Publishers
import hydro.common.publisher.TriggerablePublisher
import hydro.common.time.Clock
import hydro.common.LoggingUtils.logExceptions
import hydro.controllers.helpers.AuthenticatedAction
import hydro.models.access.DbQueryImplicits._
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.sys.process.Process
import scala.sys.process.ProcessIO

final class Application @Inject()(
    implicit override val messagesApi: MessagesApi,
    playConfiguration: play.api.Configuration,
    components: ControllerComponents,
    clock: Clock,
    entityAccess: JvmEntityAccess,
    scalaJsApiServerFactory: ScalaJsApiServerFactory,
    env: play.api.Environment,
    executionContext: ExecutionContext,
    mediaScanner: MediaScanner,
    artistAssignerFactory: ArtistAssignerFactory,
    albumParser: AlbumParser,
    storedMediaSyncer: StoredMediaSyncer,
    mediaConfiguration: MediaConfiguration,
) extends AbstractController(components)
    with I18nSupport {

  def songFile(songId: Long) = AuthenticatedAction { implicit user => implicit request =>
    val song = entityAccess.newQuerySync[Song]().findById(songId)
    val album = entityAccess.newQuerySync[Album]().findById(song.albumId)
    val relativePath = RelativePaths.joinPaths(album.relativePath, song.filename)

    val assetPath = mediaConfiguration.mediaFolder resolve relativePath

    if (!Files.exists(assetPath)) {
      NotFound(s"Could not find $assetPath")
    } else if (Files.isDirectory(assetPath)) {
      NotFound(s"Could not find $assetPath")
    } else {
      val connection = assetPath.toFile.toURI.toURL.openConnection()
      val stream = connection.getInputStream
      val source = StreamConverters.fromInputStream(() => stream)
      RangeResult
        .ofSource(
          entityLength = stream.available(), // TODO: This may not be entirely accurate
          source = source,
          rangeHeader = request.headers.get(RANGE),
          fileName = None,
          contentType = None // TODO: Set content type
        )
    }
  }
}
