package app.flux.stores

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.GamepadStore
import app.flux.stores.quiz.TeamInputStore
import app.models.user.User
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.stores.ApplicationIsOnlineStore
import hydro.flux.stores.PageLoadingStateStore
import hydro.models.access.HydroPushSocketClientFactory
import hydro.models.access.JsEntityAccess
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig

final class Module(
    implicit i18n: I18n,
    user: User,
    entityAccess: JsEntityAccess,
    dispatcher: Dispatcher,
    clock: Clock,
    scalaJsApiClient: ScalaJsApiClient,
    hydroPushSocketClientFactory: HydroPushSocketClientFactory,
    getInitialDataResponse: GetInitialDataResponse,
    quizConfig: QuizConfig,
    soundEffectController: SoundEffectController,
) {

  implicit val globalMessagesStore = new GlobalMessagesStore
  implicit val pageLoadingStateStore = new PageLoadingStateStore
  implicit val pendingModificationsStore = new PendingModificationsStore
  implicit val applicationIsOnlineStore = new ApplicationIsOnlineStore
  implicit val teamsAndQuizStateStore = new TeamsAndQuizStateStore
  implicit private val gamepadStore = new GamepadStore
  implicit val teamInputStore = new TeamInputStore
}
