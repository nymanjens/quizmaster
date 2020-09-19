package app.models.quiz.export

import app.common.FixedPointNumber
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.Team

import scala.collection.immutable.Seq
import scala.util.matching.Regex

object ExportImport {

  def exportToString(fullState: FullState): String = {
    s"<~<" +
      s"${fullState.quizState.roundIndex}<~<" +
      s"${fullState.quizState.questionIndex}<~<" +
      s"${fullState.quizState.generalQuizSettings.showAnswers}<~<" +
      s"${fullState.quizState.generalQuizSettings.answerBulletType}<~<" +
      exportTeams(fullState.teams) +
      s">~>"
  }

  def importFromString(string: String): FullState = {
    val exportRegex: Regex = """<~<(-?\d+)<~<(-?\d+)<~<(\w+)<~<(\w+)<~<(.+)>~>""".r

    string.trim match {
      case exportRegex(roundIndex, questionIndex, showAnswers, answerBulletType, teamsString) =>
        FullState(
          teams = importTeams(teamsString),
          quizState = QuizState(
            roundIndex = roundIndex.toInt,
            questionIndex = questionIndex.toInt,
            generalQuizSettings = GeneralQuizSettings(
              showAnswers = showAnswers.toBoolean,
              answerBulletType = answerBulletType match {
                case "Arrows"     => AnswerBulletType.Arrows
                case "Characters" => AnswerBulletType.Characters
              },
            ),
          ),
        )
    }
  }

  private def exportTeams(teams: Seq[Team]): String = {
    teams.map(team => s"_~_${team.name}_~_${team.score}").mkString("=~=")
  }

  private def importTeams(teamsString: String): Seq[Team] = {
    val exportRegex: Regex = """_~_(.+?)_~_(-?[\d\.]+)""".r

    for ((teamString, index) <- teamsString.split("=~=").toVector.zipWithIndex)
      yield teamString match {
        case exportRegex(name, score) =>
          Team(
            name = name,
            score = FixedPointNumber(score.toDouble),
            index = index,
          )
      }
  }

  case class FullState(
      teams: Seq[Team],
      quizState: QuizState,
  )
}
