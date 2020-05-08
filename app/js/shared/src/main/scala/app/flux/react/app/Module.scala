package app.flux.react.app

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.flux.controllers.SoundEffectController
import app.flux.react.app.quiz.GamepadSetupView
import app.flux.react.app.quiz.GeneralQuizSettings
import app.flux.react.app.quiz.QuizSettingsView
import app.flux.react.app.quiz.ImportBackupPanel
import app.flux.react.app.quiz.MasterView
import app.flux.react.app.quiz.ObfuscatedAnswer
import app.flux.react.app.quiz.QuestionComponent
import app.flux.react.app.quiz.QuizProgressIndicator
import app.flux.react.app.quiz.QuizSettingsPanels
import app.flux.react.app.quiz.QuizView
import app.flux.react.app.quiz.RoundComponent
import app.flux.react.app.quiz.SubmissionsSummaryTable
import app.flux.react.app.quiz.SubmissionsSummaryTable
import app.flux.react.app.quiz.SubmissionsSummaryView
import app.flux.react.app.quiz.SyncedTimerBar
import app.flux.react.app.quiz.TeamControllerView
import app.flux.react.app.quiz.TeamEditor
import app.flux.react.app.quiz.TeamsList
import app.flux.stores._
import app.flux.stores.quiz.SubmissionsSummaryStore
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.user.User
import hydro.common.I18n
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
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    submissionsSummaryStore: SubmissionsSummaryStore,
    teamInputStore: TeamInputStore,
    dispatcher: Dispatcher,
    clock: Clock,
    scalaJsApiClient: ScalaJsApiClient,
    quizConfig: QuizConfig,
    soundEffectController: SoundEffectController,
    getInitialDataResponse: GetInitialDataResponse,
) {

  // Configuration of submodules
  private val hydroUielementsModule = new hydro.flux.react.uielements.Module
  implicit private val pageHeader = hydroUielementsModule.pageHeader
  implicit private val sbadminLayout = hydroUielementsModule.sbadminLayout

  implicit private val syncedTimerBar: SyncedTimerBar = new SyncedTimerBar()
  implicit private val obfuscatedAnswer: ObfuscatedAnswer = new ObfuscatedAnswer()
  implicit private val quizProgressIndicator: QuizProgressIndicator = new QuizProgressIndicator()
  implicit private val importBackupPanel: ImportBackupPanel = new ImportBackupPanel()
  implicit private val teamEditor: TeamEditor = new TeamEditor()
  implicit private val teamsList: TeamsList = new TeamsList()
  implicit private val generalQuizSettings: GeneralQuizSettings = new GeneralQuizSettings()
  implicit private val question: QuestionComponent = new QuestionComponent()
  implicit private val quizSettingsPanels: QuizSettingsPanels = new QuizSettingsPanels()
  implicit private val submissionsSummaryTable = new SubmissionsSummaryTable()
  implicit private val roundComponent = new RoundComponent()

  implicit val layout: Layout = new Layout
  implicit val teamController: TeamControllerView = new TeamControllerView()
  implicit val quizView: QuizView = new QuizView()
  implicit val masterView: MasterView = new MasterView()
  implicit val submissionsSummaryView: SubmissionsSummaryView = new SubmissionsSummaryView()
  implicit val gamepadSetupView: GamepadSetupView = new GamepadSetupView()
  implicit val quizSettingsView: QuizSettingsView = new QuizSettingsView()
}
