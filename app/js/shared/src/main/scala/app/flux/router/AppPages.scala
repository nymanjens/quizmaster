package app.flux.router

import hydro.flux.router.Page.PageBase

object AppPages {

  // **************** Quiz views **************** //
  case object TeamController extends PageBase("app.team", iconClass = "fa fa-group fa-fw")
  case object Quiz extends PageBase("app.quiz", iconClass = "fa fa-question-circle fa-fw")
  case object Master extends PageBase("app.master", iconClass = "fa fa-key fa-fw")
  case object SubmissionsSummary
      extends PageBase("app.summary-of-all-scores", iconClass = "fa fa-table fa-fw")
  case object Gamepad extends PageBase("app.gamepad-setup", iconClass = "fa fa-gamepad fa-fw")
  case object QuizSettings extends PageBase("app.quiz-settings", iconClass = "fa fa-cog fa-fw")
}
