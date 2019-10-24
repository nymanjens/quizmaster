package app.flux.react.app.quiz

import app.flux.stores.quiz.GamepadStore.Arrow
import hydro.flux.react.ReactVdomUtils.<<
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState.TimerState
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.VdomNode

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
              case 0 if !props.showMasterData => showSingleQuestionStep0PreparatoryTitle(single)
              case 0 if props.showMasterData  => showSingleQuestion(single)
              case 1 | 2 | 3 | 4              => showSingleQuestion(single)
            }

          case double: Question.Double => showDoubleQuestion(double)
        },
      )
    }

    private def showSingleQuestionStep0PreparatoryTitle(question: Question.Single)(
        implicit props: Props): VdomElement = {
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

      def ifVisibleOrMaster(isVisible: Boolean)(vdomNode: VdomNode): VdomNode = {
        if (isVisible) {
          vdomNode
        } else if (props.showMasterData) {
          <.span(^.className := "admin-only-data", vdomNode)
        } else {
          VdomArray.empty()
        }
      }

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
          <<.ifDefined(question.image) { imageFilename =>
            <.div(
              ^.className := "image-holder",
              <.img(
                ^.src := s"/quizimages/$imageFilename",
              )
            ),
          },
          <<.ifDefined(question.choices) { choices =>
            ifVisibleOrMaster(question.choicesAreVisible(progressIndex)) {
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
            showSubmissions(state.quizState.submissions),
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
              obfuscatedAnswer(question.answer)
            }
          }
        },
        <<.ifThen(question.shouldShowTimer(props.questionProgressIndex)) {
          <.div(
            ^.className := "timer",
            syncedTimerBar(maxTime = question.maybeMaxTime.get),
          )
        }
      )
    }

    private def showDoubleQuestion(question: Question.Double)(implicit props: Props): VdomElement = {
      <.div(
        ^.className := "question",
      )
    }

    private def pointsMetadata(question: Question): VdomElement = {
      <.div(
        ^.className := "points-metadata",
        if (question.onlyFirstGainsPoints) {
          if (question.pointsToGain == 1) i18n("app.first-right-answer-gains-1-point")
          else i18n("app.first-right-answer-gains-n-points", question.pointsToGain)
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
        for (submission <- submissions)
          yield
            TeamIcon(state.teams.find(_.id == submission.teamId).get)(
              ^.key := submission.teamId,
            )
      )
    }
  }
}
