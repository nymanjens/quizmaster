package app.flux.react.app.quiz

import hydro.flux.react.ReactVdomUtils.<<
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState.TimerState
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
  def apply(question: Question, questionProgressIndex: Int, showMasterData: Boolean): VdomElement = {
    component(
      Props(
        question = question,
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
          case single: Question.Single => showSingleQuestion(single)
          case double: Question.Double => showDoubleQuestion(double)
        },
      )
    }

    private def showSingleQuestion(question: Question.Single)(implicit props: Props): VdomElement = {
      val pointsString = if (question.pointsToGain == 1) "1 point" else s"${question.pointsToGain} points"
      <.div(
        <.div(
          ^.className := "question",
          question.question
        ),
        <.div(
          ^.className := "metadata",
          if (question.onlyFirstGainsPoints) {
            s"First right answer gains $pointsString"
          } else {
            s"All right answers gain $pointsString"
          },
        ),
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
  }
}
