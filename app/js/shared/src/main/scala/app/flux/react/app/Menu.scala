package app.flux.react.app

import app.flux.router.AppPages
import app.flux.stores.media.PlayStatusStore
import hydro.flux.react.uielements.SbadminMenu
import hydro.flux.react.uielements.SbadminMenu.MenuItem
import hydro.flux.router.RouterContext
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[app] final class Menu(implicit playStatusStore: PlayStatusStore, sbadminMenu: SbadminMenu) {

  // **************** API ****************//
  def apply()(implicit router: RouterContext): VdomElement = {
    sbadminMenu(
      menuItems = Seq(
        Seq(
          MenuItem("<u>P</u>laylist", AppPages.Playlist, shortcuts = Seq("shift+alt+p")),
        ),
      ),
      enableSearch = true,
      router = router,
      configureAdditionalKeyboardShortcuts = () => configureAdditionalKeyboardShortcuts(),
    )
  }

  private def configureAdditionalKeyboardShortcuts()(implicit router: RouterContext): Unit = {
    def bind(shortcut: String, runnable: () => Unit): Unit = {
      Mousetrap.bind(shortcut, e => {
        e.preventDefault()
        runnable()
      })
    }
    def bindGlobal(shortcut: String, runnable: () => Unit): Unit = {
      Mousetrap.bindGlobal(shortcut, e => {
        e.preventDefault()
        runnable()
      })
    }

    bind("space", () => playStatusStore.togglePlay())
    bind("ctrl+left", () => playStatusStore.advanceEntriesInPlaylist(step = -1))
    bind("ctrl+right", () => playStatusStore.advanceEntriesInPlaylist(step = +1))
    bindGlobal("ctrl+shift+space", () => playStatusStore.toggleStopAfterCurrentSong())
  }
}
