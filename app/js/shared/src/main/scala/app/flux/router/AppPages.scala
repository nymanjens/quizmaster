package app.flux.router

import hydro.flux.router.Page.PageBase

object AppPages {

  // **************** Quiz views **************** //
  case object Quiz extends PageBase("app.quiz", iconClass = "fa fa-question-circle fa-fw")
  case class Master(masterSecret: String) extends PageBase("app.master", iconClass = "fa fa-key fa-fw")
  case object Gamepad extends PageBase("app.gamepad-setup", iconClass = "fa fa-gamepad fa-fw")
  case class QuizSettings(masterSecret: String)
      extends PageBase("app.quiz-settings", iconClass = "fa fa-cog fa-fw")
}
