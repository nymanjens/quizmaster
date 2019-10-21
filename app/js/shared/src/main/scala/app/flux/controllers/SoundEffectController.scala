package app.flux.controllers

import app.flux.stores.quiz.TeamsAndQuizStateStore
import hydro.flux.action.Action
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.flux.stores.PageLoadingStateStore.State
import hydro.models.access.JsEntityAccess

class SoundEffectController(
    implicit dispatcher: Dispatcher,
    entityAccess: JsEntityAccess,
) {
  private var currentPage: Page = _

  dispatcher.registerPartialSync(dispatcherListener)

  // **************** Public API ****************//
  //

  // **************** Private helper methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case StandardActions.SetPageLoadingState( /* isLoading = */ _, currentPage) =>
      this.currentPage = currentPage
  }
}
