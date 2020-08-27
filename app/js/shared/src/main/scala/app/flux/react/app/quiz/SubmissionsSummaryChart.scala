package app.flux.react.app.quiz

import scala.scalajs.js
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
      <.div(
        ^.style := js.Dictionary("maxWidth" -> "1300px"),
        Recharts.ResponsiveContainer(width = "100%", height = 350)(
          Recharts.LineChart(
            data = assembleData(),
            margin = Recharts.Margin(top = 5, right = 20, left = 0, bottom = 35),
          )(
            Recharts.CartesianGrid(strokeDasharray = "3 3", vertical = false),
            Recharts.XAxis(dataKey = "name"),
            Recharts.YAxis(),
            Recharts.Tooltip(),
            Recharts.Legend(),
            (for (team <- state.teams)
              yield
                Recharts.Line(
                  tpe = "linear",
                  dataKey = team.name,
                  stroke = TeamIcon.colorOf(team),
                )).toVdomArray,
          ),
        )
      )
    }

    private def assembleData()(implicit state: State): Seq[Map[String, js.Any]] = {
      for {
        (round, roundIndex) <- quizConfig.rounds.zipWithIndex
        (question, questionIndex) <- round.questions.zipWithIndex
      } yield {
        val teamsScores: Map[String, js.Any] = state.teams.map(t => t.name -> (0: js.Any)).toMap
        Map[String, js.Any]("name" -> s"${roundIndex + 1}.${questionIndex + 1}") ++ teamsScores
      }
    }
  }
}
