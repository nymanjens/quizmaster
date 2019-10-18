package app.flux.react.app.quiz

import hydro.flux.react.ReactVdomUtils.<<
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState.TimerState
import app.models.quiz.config.QuizConfig.Round
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class QuestionComponent(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    syncedTimerBar: SyncedTimerBar,
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
        _.copy(timerState = teamsAndQuizStateStore.stateOrEmpty.quizState.timerState))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      question: Question,
      round: Round,
      questionProgressIndex: Int,
      showMasterData: Boolean,
  )
  protected case class State(timerState: TimerState = TimerState.nullInstance)

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val _: Props = props
      <.div(
        ^.className := "question-wrapper",
        props.question match {
          case single: Question.Single =>
            props.questionProgressIndex match {
              case 0         => showSingleQuestionStep0PreparatoryTitle(single)
              case 1 | 2 | 3 => showSingleQuestion(single)
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
          s"Question $questionNumber"
        ),
        pointsMetadata(question),
      )
    }

    private def showSingleQuestion(question: Question.Single)(implicit props: Props): VdomElement = {
      <.div(
        <.div(
          ^.className := "question",
          question.question
        ),
        pointsMetadata(question),
        <<.ifThen(question.isBeingAnswered(props.questionProgressIndex)) {
          <<.ifDefined(question.maxTime) { maxTime =>
            <.div(
              ^.className := "timer",
              syncedTimerBar(maxTime = maxTime),
            )
          }
        }
      )
    }

    private def showDoubleQuestion(question: Question.Double)(implicit props: Props): VdomElement = {
      <.div(
        ^.className := "question",
      )
    }

    private def pointsMetadata(question: Question): VdomElement = {
      val pointsString = if (question.pointsToGain == 1) "1 point" else s"${question.pointsToGain} points"
      <.div(
        ^.className := "points-metadata",
        if (question.onlyFirstGainsPoints) {
          s"First right answer gains $pointsString"
        } else {
          s"All right answers gain $pointsString"
        },
      )
    }
  }
}
