package app.flux.react.app.quiz

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.concurrent.Future

final class TeamControllerView(
    implicit pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamEditor: TeamEditor,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizProgressIndicator: QuizProgressIndicator,
    questionComponent: QuestionComponent,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      state =>
        state.copy(
          quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
          teams = teamsAndQuizStateStore.stateOrEmpty.teams,
          // Update the team, e.g. in case the name changed
          maybeTeam =
            state.maybeTeam.map(team => teamsAndQuizStateStore.stateOrEmpty.teams.find(_.id == team.id).get)
      )
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      quizState: QuizState = QuizState.nullInstance,
      teams: Seq[Team] = Seq(),
      maybeTeam: Option[Team] = None,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    val teamNameInputRef = TextInput.ref()

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router
      implicit val _: State = state
      implicit val quizState = state.quizState

      <.span(
        ^.className := "team-controller-view",
        state.maybeTeam match {
          case None       => createTeamForm()
          case Some(team) => controller(team)
        }
      )
    }

    private def createTeamForm()(implicit state: State): VdomNode = {
      <.form(
        Bootstrap.FormGroup(
          <.label(i18n("app.team-name")),
          <.div(
            TextInput(
              ref = teamNameInputRef,
              name = "team-name",
              focusOnMount = true,
            ),
          ),
        ),
        <.div(
          Bootstrap.Button(Variant.primary, Size.sm, tpe = "submit")(
            i18n("app.submit"),
            ^.onClick ==> { (e: ReactEventFromInput) =>
              e.preventDefault()
              val name = teamNameInputRef().valueOrDefault
              if (name.nonEmpty) {
                Callback.future {
                  val teamFuture = state.teams.find(_.name == name) match {
                    case None       => teamsAndQuizStateStore.addTeam(name = name)
                    case Some(team) => Future.successful(team)
                  }
                  teamFuture.map(team => $.modState(_.copy(maybeTeam = Some(team))))
                }
              } else {
                Callback.empty
              }
            }
          ),
        ),
      )
    }

    private def controller(team: Team)(implicit quizState: QuizState): VdomNode = {
      <.span(
        <.div(^.className := "team-name", team.name),
        quizProgressIndicator(quizState, showMasterData = false),
        quizState.maybeQuestion match {
          case None =>
            <.span("Waiting for the next question...")
          case Some(question) =>
            <.span("Question")
        },
      )
    }
  }
}
