package app.flux.react.app.quiz

import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.<<
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class RoundComponent(
    implicit
    quizConfig: QuizConfig,
    i18n: I18n,
    submissionsSummaryTable: SubmissionsSummaryTable,
    submissionsSummaryChart: SubmissionsSummaryChart,
) {
  def apply(round: QuizConfig.Round, showMasterData: Boolean = false)(
      implicit quizState: QuizState): VdomElement = {
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
      <<.ifThen(showMasterData && round.questions.nonEmpty) {
        <.div(
          ^.className := "round-metadata",
          s"${i18n("app.max-points-to-gain")}: ${round.questions.map(_.defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer = true)).sum}",
        )
      },
      <<.ifThen(quizState.quizIsBeingSetUp) {
        <<.ifDefined(quizConfig.author) { author =>
          <.div(
            ^.className := "round-metadata",
            author,
          )
        }
      },
      <<.ifThen(quizState.quizIsBeingSetUp && showMasterData) {
        <.div(
          ^.className := "round-help",
          i18n("app.first-round-help"),
        )
      },
      <<.ifThen(quizState.quizHasEnded) {
        <.div(
          submissionsSummaryChart(selectedTeamId = None),
          submissionsSummaryTable(selectedTeamId = None),
        )
      },
    )
  }
}
