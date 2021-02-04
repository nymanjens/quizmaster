package app.flux.react.app.quiz

import app.models.quiz.Team
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

object TeamIcon {

  private val colors: Seq[String] =
    Seq("red", "orange", "blue", "green", "deeppink", "#DD0", "purple", "fuchsia")
  private val icons: Seq[VdomTag] = Seq(
    Bootstrap.FontAwesomeIcon("paw", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("rocket", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("truck", fixedWidth = true),
    Bootstrap.Glyphicon("knight"),
    Bootstrap.Glyphicon("grain"),
    Bootstrap.FontAwesomeIcon("flask", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("flag", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("cloud", fixedWidth = true),
    Bootstrap.Glyphicon("piggy-bank"),
    Bootstrap.Glyphicon("sunglasses"),
    Bootstrap.FontAwesomeIcon("shopping-cart", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("coffee", fixedWidth = true),
    Bootstrap.Glyphicon("tree-deciduous"),
    Bootstrap.FontAwesomeIcon("beer", fixedWidth = true),
    Bootstrap.FontAwesomeIcon("tree", fixedWidth = true),
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
