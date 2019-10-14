package app.flux.router

import hydro.common.I18n
import hydro.flux.router.Page
import hydro.flux.router.Page.PageBase
import hydro.models.access.EntityAccess

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object AppPages {

  // **************** Media views **************** //
  case object Playlist extends PageBase("app.playlist", iconClass = "icon-list")

  case class Artist(artistId: Long) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      entityAccess.newQuery[app.models.media.Artist]().findById(artistId).map(_.name)
    override def iconClass = "fa fa-user fa-fw"
  }
  case class Album(albumId: Long) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      entityAccess.newQuery[app.models.media.Album]().findById(albumId).map(_.title)
    override def iconClass = "glyphicon glyphicon-cd"
  }
}
