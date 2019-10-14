package app.models.access

import app.models.access.ModelFields.QuizState
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

  protected def getEntityTableDef(entityType: EntityType.any): SlickEntityTableDef[entityType.get] = ???
}
