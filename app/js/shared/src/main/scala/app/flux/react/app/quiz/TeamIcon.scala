package app.flux.react.app.quiz

import app.models.quiz.Team
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

object TeamIcon {

  private val colors: Seq[String] =
    Seq("red", "orange", "blue", "green", "deeppink", "#DD0", "purple", "fuchsia")
  private val icons: Seq[VdomTag] = Seq(
    Bootstrap.FontAwesomeIcon("bomb", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("beer", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("truck", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("flag", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("anchor", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("road", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("shopping-cart", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("cutlery", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("shield", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("flask", fixedWidth = true),
  )

  def apply(team: Team): VdomTag = {
    icons(team.index % icons.size)(
      ^.style := js.Dictionary("color" -> colorOf(team))
    )
  }

  def colorOf(team: Team): String = {
    colors(team.index % colors.size)
  }
}
