package app.flux.react.app.quiz

import app.models.quiz.config.QuizConfig
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.<<
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

object RoundComponent {
  def apply(round: QuizConfig.Round, showMasterData: Boolean = false)(implicit i18n: I18n): VdomElement = {
    <.div(
      ^.className := "round-wrapper",
      <.div(
        ^.className := "round-title",
        round.name,
      ),
      <<.ifThen(showMasterData) {
        <<.ifDefined(round.expectedTime) { expectedTime =>
          <.div(
            ^.className := "round-metadata",
            i18n("app.expected-minutes", expectedTime.toMinutes)
          )
        }
      },
      <<.ifThen(showMasterData) {
        <.div(
          ^.className := "round-metadata",
          s"${i18n("app.max-points-to-gain")}: ${round.questions.map(_.pointsToGainOnFirstAnswer).sum}",
        )
      },
    )
  }
}
