package app.flux.router

import hydro.flux.router.Page.PageBase

object AppPages {

  // **************** Quiz views **************** //
  case object Quiz extends PageBase("app.quiz", iconClass = "")
  case class Master(masterSecret: String) extends PageBase("app.master", iconClass = "")
  case object Gamepad extends PageBase("app.gamepad-setup", iconClass = "fa fa-gamepad")
  case object QuizSettings extends PageBase("app.settings", iconClass = "fa fa-cog")
}
