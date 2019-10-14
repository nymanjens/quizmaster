package app.models.quiz.config

import app.models.quiz.config.QuizConfig.Round

import scala.concurrent.duration._
import scala.concurrent.duration.Duration

case class QuizConfig(
    rounds: Seq[Round],
)

object QuizConfig {

  case class Round(
      name: String,
      questions: Seq[Question],
  )

  sealed trait Question

  object Question {
    case class Single(
        question: String,
        answer: String,
        choices: Option[Seq[String]],
        pointsToGain: Int,
        maxTime: Option[Duration],
        onlyFirstGainsPoints: Boolean,
    ) extends Question

    case class Double(
        verbalQuestion: String,
        verbalAnswer: String,
        textualQuestion: String,
        textualAnswer: String,
        textualChoices: Seq[String],
        pointsToGain: Int,
    ) extends Question {
      def maxTime: Duration = 3.seconds
    }
  }
}
