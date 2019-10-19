package app.flux.react.app.quiz

import app.models.quiz.Team
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

object TeamIcon {

  private val colors: Seq[String] = Seq("red", "orange", "blue", "deeppink", "green")
  private val icons: Seq[VdomTag] = Seq(
    Bootstrap.FontAwesomeIcon("bomb", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("beer", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("flag", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("anchor", fixedWidth = true),
  )

  def apply(team: Team): VdomTag = {
    icons(team.index % icons.size)(
      ^.style := js.Dictionary("color" -> colors(team.index % colors.size))
    )
  }
}
