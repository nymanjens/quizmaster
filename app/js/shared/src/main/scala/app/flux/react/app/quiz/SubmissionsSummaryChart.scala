package app.flux.react.app.quiz

import app.common.FixedPointNumber
import app.flux.stores.quiz.SubmissionsSummaryStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.jsfacades.Recharts
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.collection.immutable.Seq

final class SubmissionsSummaryChart(
    implicit quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    submissionsSummaryStore: SubmissionsSummaryStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(selectedTeamId: Option[Long]): VdomElement = {
    component(Props(selectedTeamId = selectedTeamId))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(teams = teamsAndQuizStateStore.stateOrEmpty.teams))
      .withStateStoresDependency(
        submissionsSummaryStore,
        _.copy(submissionsSummaryState = submissionsSummaryStore.stateOrEmpty))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(selectedTeamId: Option[Long])
  protected case class State(
      teams: Seq[Team] = Seq(),
      submissionsSummaryState: SubmissionsSummaryStore.State = SubmissionsSummaryStore.State.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomNode = logExceptions {
      implicit val _ = props
      implicit val __ = state
      Recharts.ResponsiveContainer(width = "100%", height = 500)(
        Recharts.LineChart(
          width = 500,
          height = 500,
          data = Seq(
            Map("name" -> "Page A", "uv" -> 4000, "pv" -> 2400, "amt" -> 2400),
            Map("name" -> "Page B", "uv" -> 3000, "pv" -> 1398, "amt" -> 2210),
            Map("name" -> "Page C", "uv" -> 2000, "pv" -> 9800, "amt" -> 2290),
            Map("name" -> "Page D", "uv" -> 2780, "pv" -> 3908, "amt" -> 2000),
            Map("name" -> "Page E", "uv" -> 1890, "pv" -> 4800, "amt" -> 2181),
            Map("name" -> "Page F", "uv" -> 2390, "pv" -> 3800, "amt" -> 2500),
            Map("name" -> "Page G", "uv" -> 3490, "pv" -> 4300, "amt" -> 2100),
          ),
          margin = Recharts.Margin(top = 5, right = 30, left = 20, bottom = 5),
        )(
          Recharts.CartesianGrid(strokeDasharray = "3 3", vertical = false),
          Recharts.XAxis(dataKey = "name"),
          Recharts.YAxis(),
          Recharts.Tooltip(),
          Recharts.Legend(),
          Recharts.Line(
            tpe = "linear",
            dataKey = "pv",
            stroke = "blue",
          ),
          Recharts.Line(
            tpe = "linear",
            dataKey = "uv",
            stroke = "green",
          ),
        ),
      )
    }
  }
}
