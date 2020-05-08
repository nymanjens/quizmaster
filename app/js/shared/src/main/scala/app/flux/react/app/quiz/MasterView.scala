package app.flux.react.app.quiz

import app.api.ScalaJsApi.GetInitialDataResponse
import app.common.LocalStorageClient
import app.common.MasterSecretUtils
import app.flux.router.AppPages
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class MasterView(
    implicit pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    quizSettingsPanels: QuizSettingsPanels,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizProgressIndicator: QuizProgressIndicator,
    questionComponent: QuestionComponent,
    getInitialDataResponse: GetInitialDataResponse,
    submissionsSummaryTable: SubmissionsSummaryTable,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    MasterSecretUtils.requireMasterSecretOrRedirect(component(Props(router)), router)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        teams = teamsAndQuizStateStore.stateOrEmpty.teams,
        quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
      quizState: QuizState = QuizState.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        ^.className := "master-view",
        quizNavigationButtons(state.quizState),
        quizProgressIndicator(state.quizState, showMasterData = true),
        state.quizState match {
          case quizState if quizState.quizIsBeingSetUp =>
            quizSettingsPanels()
          case quizState =>
            quizState.maybeQuestion match {
              case None if quizState.quizHasEnded =>
                <.span(
                  RoundComponent(quizState.round),
                  submissionsSummaryTable(selectedTeamId = None),
                )
              case None => RoundComponent(quizState.round, showMasterData = true)
              case Some(question) =>
                questionComponent(
                  question = question,
                  round = state.quizState.round,
                  questionProgressIndex = quizState.questionProgressIndex,
                  showMasterData = true,
                )
            }
        },
      )
    }

    private def quizNavigationButtons(quizState: QuizState): VdomTag = {
      <.div(
        ^.className := "quiz-navigation-buttons",
        <<.ifThen(!quizState.quizIsBeingSetUp) {
          <.span(
            Bootstrap.Button(Variant.primary, Size.lg)(
              ^.onClick --> LogExceptionsCallback(teamsAndQuizStateStore.goToPreviousStep()).void,
              Bootstrap.Glyphicon("arrow-left"),
              " Previous",
            ),
            " ",
          )
        },
        <<.ifThen(!quizState.quizHasEnded) {
          Bootstrap.Button(Variant.primary, Size.lg)(
            ^.onClick --> LogExceptionsCallback(teamsAndQuizStateStore.goToNextStep()).void,
            Bootstrap.Glyphicon("arrow-right"),
            " Next",
          )
        }
      )
    }
  }
}
