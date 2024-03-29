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

final class MasterView(implicit
    pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    quizSettingsPanels: QuizSettingsPanels,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    questionComponent: QuestionComponent,
    getInitialDataResponse: GetInitialDataResponse,
    roundComponent: RoundComponent,
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
      ),
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
      quizState: QuizState = QuizState.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.div(
        ^.className := "master-view",
        masterBanner(),
        quizNavigationButtons(state.quizState),
        state.quizState.maybeQuestion match {
          case None => roundComponent(state.quizState.round, showMasterData = true)(state.quizState)
          case Some(question) =>
            questionComponent(
              showMasterData = true,
              quizState = state.quizState,
              teams = state.teams,
            )
        },
        keyboardShortcutsHelp(state.quizState),
      )
    }

    private def masterBanner(): VdomTag = {
      <.div(
        ^.className := "banner-container",
        <.div(
          ^.className := "banner",
          "Master",
        ),
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
              " Previous (",
              <.kbd(Bootstrap.FontAwesomeIcon("caret-left")),
              ")",
            ),
            " ",
          )
        },
        <<.ifThen(!quizState.quizHasEnded) {
          Bootstrap.Button(Variant.primary, Size.lg)(
            ^.onClick --> LogExceptionsCallback(teamsAndQuizStateStore.goToNextStep()).void,
            Bootstrap.Glyphicon("arrow-right"),
            " Next (",
            <.kbd(Bootstrap.FontAwesomeIcon("caret-right")),
            ")",
          )
        },
      )
    }

    private def keyboardShortcutsHelp(quizState: QuizState): VdomTag = {
      def rowSet(topic: String, namesAndShortcuts: (String, VdomTag)*): VdomArray = {
        (for (((name, shortcut), i) <- namesAndShortcuts.zipWithIndex)
          yield <.tr(
            ^.key := name + i,
            <<.ifThen(i == 0) {
              <.td(^.rowSpan := namesAndShortcuts.size, topic)
            },
            <.td(name),
            <.td(shortcut),
          )).toVdomArray
      }

      val alt = <.kbd("alt")
      val shift = <.kbd("shift")
      val left = <.kbd(Bootstrap.FontAwesomeIcon("caret-left"))
      val right = <.kbd(Bootstrap.FontAwesomeIcon("caret-right"))

      <.div(
        ^.className := "keyboard-shortcuts-help",
        <.table(
          ^.className := "table",
          <.thead(
            <.tr(
              <.th(^.colSpan := 2, "Keyboard Shortcuts")
            )
          ),
          <.tbody(
            <<.ifThen(quizState.maybeQuestion.exists(q => q.audioSrc.isDefined))(
              rowSet("Audio", "restart" -> <.span(shift, " + ", <.kbd("R")))
            ),
            <<.ifThen(quizState.maybeQuestion.exists(q => q.image.isDefined || q.answerImage.isDefined))(
              rowSet("Image", "toggle enlarged" -> <.span(alt, " + ", <.kbd("enter")))
            ),
            <<.ifThen(
              quizState.maybeQuestion.exists(
                _.shouldShowTimer(questionProgressIndex = quizState.questionProgressIndex)
              )
            )(
              rowSet(
                "Timer",
                "pause/resume" -> <.kbd("spacebar"),
                "subtract 10s" -> <.span(alt, " + ", shift, " + ", <.kbd("-"), "/", <.kbd("O")),
                "add 10s" -> <.span(alt, " + ", shift, " + ", <.kbd("+"), "/", <.kbd("P")),
                "subtract 30s" -> <.span(shift, " + ", <.kbd("-"), "/", <.kbd("O")),
                "add 30s" -> <.span(shift, " + ", <.kbd("+"), "/", <.kbd("P")),
              )
            ),
            rowSet(
              "Navigation",
              "Go to previous/next question" -> <.span(alt, " + ", left, "/", right),
              "Go to previous/next round" -> <.span(alt, " + ", shift, " + ", left, "/", right),
            ),
          ),
        ),
      )
    }
  }
}
