package app.models.quiz.config

import app.models.quiz.config.QuizConfig.Round

import java.time.Duration

case class QuizConfig(
    rounds: Seq[Round],
)

object QuizConfig {

  case class Round(
      name: String,
      questions: Seq[Question],
  )

  sealed trait Question {
    def questionProgressSize: Int
    def isBeingAnswered(questionProgressIndex: Int): Boolean
  }

  object Question {
    case class Single(
        question: String,
        answer: String,
        choices: Option[Seq[String]],
        pointsToGain: Int,
        maxTime: Option[Duration],
        onlyFirstGainsPoints: Boolean,
    ) extends Question {
      override def questionProgressSize: Int = {
        if (choices.isDefined) 3 else 2
      }
      override def isBeingAnswered(questionProgressIndex: Int): Boolean = {
        if (choices.isDefined) {
          questionProgressIndex == 1
        } else {
          questionProgressIndex == 0
        }
      }
    }

    case class Double(
        verbalQuestion: String,
        verbalAnswer: String,
        textualQuestion: String,
        textualAnswer: String,
        textualChoices: Seq[String],
        pointsToGain: Int,
    ) extends Question {
      override def questionProgressSize: Int = 3
      override def isBeingAnswered(questionProgressIndex: Int): Boolean = questionProgressIndex == 1
      def maxTime: Duration = Duration.ofSeconds(3)
    }
  }
}
