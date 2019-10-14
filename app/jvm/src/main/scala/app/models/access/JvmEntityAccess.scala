package app.models.access

import app.models.media.Album
import app.models.media.Artist
import app.models.media.PlaylistEntry
import app.models.media.SongAnnotation
import app.models.media.PlayStatus
import app.models.media.Song
import app.models.slick.SlickEntityTableDefs
import app.models.user.User
import com.google.inject._
import hydro.common.time.Clock
import hydro.models.access.JvmEntityAccessBase
import hydro.models.modification.EntityType
import hydro.models.slick.SlickEntityTableDef

final class JvmEntityAccess @Inject()(implicit clock: Clock) extends JvmEntityAccessBase {

  protected def getEntityTableDef(entityType: EntityType.any): SlickEntityTableDef[entityType.get] = {
    val tableDef = entityType match {
      case User.Type           => SlickEntityTableDefs.UserDef
      case Song.Type           => SlickEntityTableDefs.SongDef
      case Album.Type          => SlickEntityTableDefs.AlbumDef
      case Artist.Type         => SlickEntityTableDefs.ArtistDef
      case PlaylistEntry.Type  => SlickEntityTableDefs.PlaylistEntryDef
      case PlayStatus.Type     => SlickEntityTableDefs.PlayStatusDef
      case SongAnnotation.Type => SlickEntityTableDefs.SongAnnotationDef
    }
    tableDef.asInstanceOf[SlickEntityTableDef[entityType.get]]
  }
}
