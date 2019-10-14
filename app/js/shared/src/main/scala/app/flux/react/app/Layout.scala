package app.flux.react.app

import app.flux.react.uielements
import hydro.flux.react.uielements.SbadminLayout
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

final class Layout(
    implicit menu: Menu,
    sbadminLayout: SbadminLayout,
    musicPlayerDiv: uielements.media.MusicPlayerDiv,
) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      implicit val router = props.router
      sbadminLayout(
        title = "Playlist Keeper",
        leftMenu = menu(),
        pageContent = <.span(children),
        extraFooter = Seq(
          musicPlayerDiv(),
          // Add extra whitespace to make sure the MusicPlayerDiv isn't blocking any content
          <.div(^.style := js.Dictionary("paddingTop" -> "200px")),
        )
      )
    }
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
