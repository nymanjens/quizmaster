package app.flux.react.app.quiz

import app.models.quiz.config.QuizConfig
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

object RoundComponent {
  def apply(round: QuizConfig.Round): VdomElement = {
    <.div(
      ^.className := "round-title",
      round.name,
    )
  }
}
