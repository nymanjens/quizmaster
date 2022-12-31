package app.flux.react.app.quiz

import app.common.JsQuizAssets
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.config.QuizConfig.Image
import hydro.common.I18n
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class RoundComponent(implicit
    quizConfig: QuizConfig,
    i18n: I18n,
    submissionsSummaryTable: SubmissionsSummaryTable,
    submissionsSummaryChart: SubmissionsSummaryChart,
) {
  def apply(round: QuizConfig.Round, showMasterData: Boolean = false)(implicit
      quizState: QuizState
  ): VdomElement = {
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
            i18n("app.expected-minutes", expectedTime.toMinutes),
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
            <<.nl2BrBlockWithLinks(author),
          )
        }
      },
      <<.ifThen(quizState.quizIsBeingSetUp) {
        <<.ifDefined(quizConfig.instructionsOnFirstSlide) { instructionsOnFirstSlide =>
          <.div(
            ^.className := "round-metadata",
            <<.nl2BrBlockWithLinks(instructionsOnFirstSlide),
          )
        }
      },
      <<.ifThen(quizState.quizIsBeingSetUp && showMasterData) {
        <.div(
          ^.className := "round-help",
          i18n("app.first-round-help"),
        )
      },
      <<.ifDefined(round.image)(img => imageComponent(img, showMasterData)),
      <<.ifThen(quizState.quizHasEnded) {
        <.div(
          submissionsSummaryChart(selectedTeamId = None),
          submissionsSummaryTable(selectedTeamId = None),
        )
      },
    )
  }

  def imageComponent(image: Image, showMasterData: Boolean)(implicit quizState: QuizState): VdomNode = {
    <.div(
      ^.className := "image-and-choices-row",
      <.div(
        ^.className := "image-holder",
        ^.className := image.size,
        <.img(
          ^.src := s"/quizassets/${JsQuizAssets.encodeSource(image.src)}",
          ^.className := image.size,
          ^^.ifThen(quizState.imageIsEnlarged) {
            if (showMasterData) {
              ^.className := "indicate-enlarged"
            } else {
              ^.className := "enlarged"
            }
          },
        ),
      ),
    )
  }
}
