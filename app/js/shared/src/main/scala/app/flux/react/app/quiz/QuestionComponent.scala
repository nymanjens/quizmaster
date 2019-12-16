package app.flux.react.app.quiz

import app.flux.stores.quiz.GamepadStore.Arrow
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

final class QuestionComponent(
    implicit pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    syncedTimerBar: SyncedTimerBar,
    obfuscatedAnswer: ObfuscatedAnswer,
    clock: Clock,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(
      question: Question,
      round: Round,
      questionProgressIndex: Int,
      showMasterData: Boolean,
  ): VdomElement = {
    component(
      Props(
        question = question,
        round = round,
        questionProgressIndex = questionProgressIndex,
        showMasterData = showMasterData,
      ))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(
          quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
          teams = teamsAndQuizStateStore.stateOrEmpty.teams,
        ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      question: Question,
      round: Round,
      questionProgressIndex: Int,
      showMasterData: Boolean,
  )
  protected case class State(
      quizState: QuizState = QuizState.nullInstance,
      teams: Seq[Team] = Seq(),
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val _1: Props = props
      implicit val _2: State = state
      <.div(
        ^.className := "question-wrapper",
        props.question match {
          case single: Question.Single =>
            props.questionProgressIndex match {
              case 0 if !props.showMasterData => showPreparatoryTitle(single)
              case _                          => showSingleQuestion(single)
            }

          case double: Question.Double =>
            props.questionProgressIndex match {
              case 0 if !props.showMasterData => showPreparatoryTitle(double)
              case _                          => showDoubleQuestion(double)
            }
        },
      )
    }

    private def showPreparatoryTitle(question: Question)(implicit props: Props): VdomElement = {
      val questionNumber = (props.round.questions indexOf question) + 1
      <.div(
        <.div(
          ^.className := "question",
          i18n("app.question"),
          " ",
          questionNumber
        ),
        pointsMetadata(question),
      )
    }

    private def showSingleQuestion(
        question: Question.Single,
    )(
        implicit props: Props,
        state: State,
    ): VdomElement = {
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)
      val showSubmissionsOnChoices = question.isMultipleChoice && (question.onlyFirstGainsPoints || answerIsVisible)
      val showGamepadIconUnderChoices =
        state.quizState.submissions.nonEmpty || (state.quizState.canSubmitResponse && question.onlyFirstGainsPoints)
      val maybeImage = if (answerIsVisible) question.answerImage orElse question.image else question.image

      <.div(
        ifVisibleOrMaster(question.questionIsVisible(progressIndex)) {
          <.div(
            ^.className := "question",
            question.question,
          )
        },
        pointsMetadata(question),
        <.div(
          ^.className := "image-and-choices-row",
          <<.ifDefined(maybeImage) { image =>
            <.div(
              ^.className := "image-holder",
              ^.className := image.size,
              <.img(
                ^.src := s"/quizimages/${image.src}",
                ^.className := image.size,
                ^^.ifThen(state.quizState.imageIsEnlarged) {
                  if (props.showMasterData) {
                    ^.className := "indicate-enlarged"
                  } else {
                    ^.className := "enlarged"
                  }
                },
              ),
            )
          },
          <<.ifDefined(question.choices) { choices =>
            ifVisibleOrMaster(question.choicesAreVisible(progressIndex)) {
              <.div(
                ^.className := "choices-holder",
                ^^.ifDefined(maybeImage) { _ =>
                  ^.className := "including-image"
                },
                <.ul(
                  ^.className := "choices",
                  (for ((choice, arrow) <- choices zip Arrow.all)
                    yield {
                      val visibleSubmissions =
                        if (showSubmissionsOnChoices)
                          state.quizState.submissions.filter(_.maybeAnswerIndex == Some(arrow.answerIndex))
                        else Seq()
                      val isCorrectAnswer = choice == question.answer
                      <.li(
                        ^.key := choice,
                        arrow.icon(
                          ^.className := "choice-arrow",
                        ),
                        if (isCorrectAnswer && (answerIsVisible || visibleSubmissions.nonEmpty)) {
                          <.span(^.className := "correct", choice)
                        } else if (!isCorrectAnswer && visibleSubmissions.nonEmpty) {
                          <.span(^.className := "incorrect", choice)
                        } else {
                          choice
                        },
                        " ",
                        showSubmissions(visibleSubmissions),
                      )
                    }).toVdomArray
                )
              )
            }
          },
        ),
        <.div(
          ^.className := "submissions-without-choices",
          ifVisibleOrMaster(showGamepadIconUnderChoices) {
            Bootstrap.FontAwesomeIcon("gamepad")
          },
          " ",
          <<.ifThen(!showSubmissionsOnChoices) {
            showSubmissions(state.quizState.submissions)
          }
        ),
        <<.ifThen(question.choices.isEmpty || !answerIsVisible) {
          ifVisibleOrMaster(answerIsVisible) {
            if (answerIsVisible) {
              <.div(
                ^.className := "answer",
                question.answer,
              )
            } else {
              <.div(obfuscatedAnswer(question.answer))
            }
          }
        },
        <<.ifThen(answerIsVisible) {
          <<.ifDefined(question.answerDetail) { answerDetail =>
            <.div(
              ^.className := "answer-detail",
              question.answerDetail,
            )
          }
        },
        <<.ifThen(question.shouldShowTimer(props.questionProgressIndex)) {
          <.div(
            ^.className := "timer",
            syncedTimerBar(maxTime = question.maxTime),
          )
        },
        <<.ifThen(question.submissionAreOpen(props.questionProgressIndex) && !props.showMasterData) {
          <<.ifDefined(question.audioSrc) { audioRelativePath =>
            audioPlayer(audioRelativePath, playing = state.quizState.timerState.timerRunning)
          }
        }
      )
    }

    private def showDoubleQuestion(
        question: Question.Double,
    )(
        implicit props: Props,
        state: State,
    ): VdomElement = {
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)
      val correctSubmissionWasEntered = state.quizState.submissions.exists(submission =>
        question.isCorrectAnswerIndex(submission.maybeAnswerIndex.get))

      <.div(
        ifVisibleOrMaster(false) {
          <.div(
            <.div(
              ^.className := "verbal-question",
              question.verbalQuestion,
            ),
            <.div(
              ^.className := "verbal-answer",
              question.verbalAnswer,
            ),
          ),
        },
        if (props.showMasterData) {
          ifVisibleOrMaster(question.questionIsVisible(progressIndex)) {
            <.div(
              ^.className := "textual-question",
              question.textualQuestion,
            )
          }
        } else {
          <<.ifThen(question.questionIsVisible(progressIndex)) {
            <.div(
              ^.className := "question",
              question.textualQuestion,
            )
          }
        },
        pointsMetadata(question),
        <.div(
          ^.className := "image-and-choices-row",
          ifVisibleOrMaster(question.choicesAreVisible(progressIndex)) {
            <.div(
              ^.className := "choices-holder",
              <.ul(
                ^.className := "choices",
                (for ((choice, arrow) <- question.textualChoices zip Arrow.all)
                  yield {
                    val submissions =
                      state.quizState.submissions.filter(_.maybeAnswerIndex == Some(arrow.answerIndex))
                    val isCorrectAnswer = choice == question.textualAnswer
                    <.li(
                      ^.key := choice,
                      arrow.icon(
                        ^.className := "choice-arrow",
                      ),
                      if (isCorrectAnswer && (answerIsVisible || submissions.nonEmpty || props.showMasterData)) {
                        <.span(^.className := "correct", choice)
                      } else if (!isCorrectAnswer && submissions.nonEmpty) {
                        <.span(^.className := "incorrect", choice)
                      } else {
                        choice
                      },
                      " ",
                      showSubmissions(submissions),
                    )
                  }).toVdomArray
              )
            )
          },
        ),
        <.div(
          ^.className := "submissions-without-choices",
          ifVisibleOrMaster(state.quizState.canSubmitResponse) {
            Bootstrap.FontAwesomeIcon("gamepad")
          }
        ),
        <<.ifThen(state.quizState.canSubmitResponse && correctSubmissionWasEntered) {
          <.div(
            ^.className := "timer",
            syncedTimerBar(maxTime = question.maxTime),
          )
        }
      )
    }

    private def ifVisibleOrMaster(isVisible: Boolean)(vdomTag: VdomTag)(implicit props: Props): VdomNode = {
      if (isVisible) {
        vdomTag
      } else if (props.showMasterData) {
        vdomTag(^.className := "admin-only-data")
      } else {
        VdomArray.empty()
      }
    }

    private def pointsMetadata(question: Question): VdomElement = {
      <.div(
        ^.className := "points-metadata",
        if (question.onlyFirstGainsPoints) {
          if (question.pointsToGain == 1) i18n("app.first-right-answer-gains-1-point")
          else i18n("app.first-right-answer-gains-n-points", question.pointsToGain)
        } else if (question.pointsToGainOnFirstAnswer != question.pointsToGain) {
          i18n(
            "app.first-answer-gains-n-points-others-m",
            question.pointsToGainOnFirstAnswer,
            question.pointsToGain)
        } else {
          if (question.pointsToGain == 1) i18n("app.all-right-answers-gain-1-point")
          else i18n("app.all-right-answers-gain-n-points", question.pointsToGain)
        },
        <<.ifThen(question.pointsToGainOnWrongAnswer != 0) {
          ". " + (
            if (question.pointsToGainOnWrongAnswer == -1) i18n("app.wrong-answer-loses-1-point")
            else i18n("app.wrong-answer-loses-n-points", -question.pointsToGainOnWrongAnswer)
          ) + "."
        }
      )
    }

    private def showSubmissions(submissions: Seq[Submission])(implicit state: State) = {
      <<.joinWithSpaces(
        for ((submission, index) <- submissions.zipWithIndex)
          yield
            TeamIcon(state.teams.find(_.id == submission.teamId).get)(
              ^.key := s"${submission.teamId}-$index",
            )
      )
    }
  }

  private def audioPlayer(audioRelativePath: String, playing: Boolean): VdomNode = {
    RawMusicPlayer(src = "/quizaudio/" + audioRelativePath, playing = playing)
  }
}
