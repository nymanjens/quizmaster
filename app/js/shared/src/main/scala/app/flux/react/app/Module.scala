package app.flux.react.app

import app.flux.react.app.quiz.MasterView
import app.flux.react.app.quiz.QuizView
import app.flux.react.app.quiz.TeamEditor
import hydro.common.I18n
import app.flux.stores._
import app.models.quiz.config.QuizConfig
import app.models.user.User
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.models.access.JsEntityAccess

final class Module(
    implicit i18n: I18n,
    user: User,
    entityAccess: JsEntityAccess,
    globalMessagesStore: GlobalMessagesStore,
    pageLoadingStateStore: PageLoadingStateStore,
    pendingModificationsStore: PendingModificationsStore,
    applicationIsOnlineStore: ApplicationIsOnlineStore,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
) {

  // Configuration of submodules
  private val hydroUielementsModule = new hydro.flux.react.uielements.Module
  implicit private lazy val pageHeader = hydroUielementsModule.pageHeader
  implicit private lazy val sbadminMenu = hydroUielementsModule.sbadminMenu
  implicit private lazy val sbadminLayout = hydroUielementsModule.sbadminLayout

  implicit lazy val layout: Layout = new Layout

  implicit lazy private val teamEditor: TeamEditor = new TeamEditor()

  implicit lazy val quizView: QuizView = new QuizView()
  implicit lazy val masterView: MasterView = new MasterView()
}
