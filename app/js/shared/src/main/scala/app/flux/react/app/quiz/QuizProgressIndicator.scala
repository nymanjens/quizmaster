package app.flux.react.app.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class QuizProgressIndicator(implicit
    pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(quizState: QuizState, showMasterData: Boolean): VdomElement = {
    component(
      Props(
        quizState = quizState,
        showMasterData = showMasterData,
      )
    )
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      quizState: QuizState,
      showMasterData: Boolean,
  )
  protected case class State()

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val quizState = props.quizState
      <.div(
        ^.className := "quiz-progres-indicator",
        quizState match {
          case _ if quizState.quizIsBeingSetUp =>
            <.span()
          case _ if quizState.quizHasEnded =>
            <.span()
          case _ =>
            <.span(
              i18n("app.round-i-of-n", quizState.roundIndex + 1, quizConfig.rounds.size),
              s" (${quizState.round.name}). ",
              if (quizState.maybeQuestion.isDefined) {
                i18n("app.question-i-of-n", quizState.questionIndex + 1, quizState.round.questions.size)
              } else {
                s"${quizState.round.questions.size} ${i18n("app.questions")}"
              },
              ". ",
              <<.ifThen(props.showMasterData) {
                <<.ifDefined(quizState.maybeQuestion) { question =>
                  {
                    for (i <- 0 to question.progressStepsCount - 1) yield {
                      Bootstrap.FontAwesomeIcon("circle")(
                        ^.key := i,
                        ^.className := (if (i <= quizState.questionProgressIndex) "seen" else "unseen"),
                      )
                    }
                  }.toVdomArray
                }
              },
            )
        },
      )
    }
  }
}
