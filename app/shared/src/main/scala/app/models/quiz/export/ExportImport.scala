package app.models.quiz.export

import app.models.quiz.QuizState
import app.models.quiz.Team

import scala.collection.immutable.Seq
import scala.util.matching.Regex

object ExportImport {

  def exportToString(fullState: FullState): String = {
    s"<~<" +
      s"${fullState.quizState.roundIndex}<~<" +
      s"${fullState.quizState.questionIndex}<~<" +
      exportTeams(fullState.teams) +
      s">~>"
  }

  def importFromString(string: String): FullState = {
    val exportRegex: Regex = """<~<(-?\d+)<~<(-?\d+)<~<(\s+)>~>""".r

    string match {
      case exportRegex(roundIndex, questionIndex, teamsString) =>
        FullState(
          teams = importTeams(teamsString),
          quizState = QuizState(
            roundIndex = roundIndex.toInt,
            questionIndex = questionIndex.toInt,
          )
        )
    }
  }

  private def exportTeams(teams: Seq[Team]): String = {
    teams.map(team => s"_~_${team.name}_~_${team.score}").mkString("=~=")
  }

  private def importTeams(teamsString: String): Seq[Team] = {
    val exportRegex: Regex = """_~_(\w+)_~_(-?\d+)""".r

    for ((teamString, index) <- teamsString.split("~=~").toVector.zipWithIndex)
      yield
        teamString match {
          case exportRegex(name, score) =>
            Team(
              name = name,
              score = score.toInt,
              index = index,
            )
        }
  }

  case class FullState(
      teams: Seq[Team],
      quizState: QuizState,
  )
}
