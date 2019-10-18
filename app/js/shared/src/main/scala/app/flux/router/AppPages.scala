package app.flux.router

import hydro.flux.router.Page.PageBase

object AppPages {

  // **************** Quiz views **************** //
  case object Quiz extends PageBase("app.quiz", iconClass = "")
  case object Master extends PageBase("app.master", iconClass = "")
  case object Gamepad extends PageBase("app.gamepad-setup", iconClass = "")
}
