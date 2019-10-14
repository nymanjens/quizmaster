package app.flux.router

import hydro.common.I18n
import hydro.flux.router.Page
import hydro.flux.router.Page.PageBase
import hydro.models.access.EntityAccess

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object AppPages {

  // **************** Quiz views **************** //
  case object Start extends PageBase("app.start", iconClass = "fa fa-user fa-fw")
}
