package app.flux.router

import hydro.flux.router.Page
import hydro.flux.router.Page.PageBase

object AppPages {

  // **************** Quiz views **************** //
  case object TeamSelection extends PageBase("app.team", iconClass = "fa fa-group fa-fw")
  case class TeamController(teamId: Long) extends PageBase("app.team", iconClass = "fa fa-group fa-fw")
  case object Quiz extends PageBase("app.quiz", iconClass = "fa fa-question-circle fa-fw")
  case object Master extends PageBase("app.master", iconClass = "fa fa-key fa-fw")
  case object SubmissionsSummary
      extends PageBase("app.summary-of-all-scores", iconClass = "fa fa-table fa-fw")
  case object Gamepad extends PageBase("app.gamepad-setup", iconClass = "fa fa-gamepad fa-fw")
  case object QuizSettings extends PageBase("app.quiz-settings", iconClass = "fa fa-cog fa-fw")

  // **************** Additional API **************** //
  def isMasterOnlyPage(page: Page): Boolean = {
    page match {
      case TeamSelection     => false
      case TeamController(_) => false
      case _                 => true
    }
  }
}
