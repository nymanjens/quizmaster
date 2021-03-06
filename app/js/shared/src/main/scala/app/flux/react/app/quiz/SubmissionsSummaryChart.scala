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
import scala.collection.mutable

final class SubmissionsSummaryChart(implicit
    quizConfig: QuizConfig,
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
        _.copy(teams = teamsAndQuizStateStore.stateOrEmpty.teams),
      )
      .withStateStoresDependency(
        submissionsSummaryStore,
        _.copy(submissionsSummaryState = submissionsSummaryStore.stateOrEmpty),
      )

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
              yield Recharts.Line(
                key = team.name,
                tpe = "linear",
                dataKey = team.name,
                stroke = TeamIcon.colorOf(team),
              )).toVdomArray,
          )
        ),
      )
    }

    private def assembleData()(implicit state: State): Seq[Map[String, js.Any]] = {
      val cumulativePoints: mutable.Map[Team, Double] = mutable.Map()
      val discretionaryPoints: Map[Team, Double] =
        state.teams.map(t => t -> (t.score - state.submissionsSummaryState.totalPoints(t)).toDouble).toMap
      val numberOfQuestions = quizConfig.rounds.flatMap(_.questions).size

      for {
        (round, roundIndex) <- quizConfig.rounds.zipWithIndex
        (question, questionIndex) <- round.questions.zipWithIndex
      } yield {
        for (team <- state.teams) {
          val newPoints =
            cumulativePoints.getOrElse(team, 0.0) +
              state.submissionsSummaryState.points(roundIndex, questionIndex, team.id).toDouble +
              (discretionaryPoints(team) / numberOfQuestions)
          cumulativePoints.put(team, newPoints)
        }

        val teamsScores =
          state.teams.map(t => t.name -> (FixedPointNumber(cumulativePoints(t)).toDouble: js.Any)).toMap
        Map[String, js.Any]("name" -> s"${roundIndex + 1}.${questionIndex + 1}") ++ teamsScores
      }
    }
  }
}
