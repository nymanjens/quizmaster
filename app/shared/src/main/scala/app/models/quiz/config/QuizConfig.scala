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
    def progressStepsCount: Int
    final def maxProgressIndex: Int = progressStepsCount - 1
    def isBeingAnswered(questionProgressIndex: Int): Boolean
    def pointsToGain: Int
    def onlyFirstGainsPoints: Boolean
  }

  object Question {
    case class Single(
        question: String,
        answer: String,
        choices: Option[Seq[String]],
        override val pointsToGain: Int,
        maxTime: Option[Duration],
        override val onlyFirstGainsPoints: Boolean,
    ) extends Question {

      /**
        * Steps:
        * 0- Show preparatory title: "Question 2"
        * 1- Show question: "This is the question, do you know the answer?"
        * 2- (if relevant) Show choices
        * 3- Show answer
        */
      override def progressStepsCount: Int = {
        if (choices.isDefined) 4 else 3
      }
      override def isBeingAnswered(questionProgressIndex: Int): Boolean = {
        if (choices.isDefined) {
          questionProgressIndex == 2
        } else {
          questionProgressIndex == 1
        }
      }
      def questionIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
      def choicesAreVisible(questionProgressIndex: Int): Boolean = {
        choices.isDefined && questionProgressIndex >= 2
      }
      def answerIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == maxProgressIndex
      }
    }

    case class Double(
        verbalQuestion: String,
        verbalAnswer: String,
        textualQuestion: String,
        textualAnswer: String,
        textualChoices: Seq[String],
        override val pointsToGain: Int,
    ) extends Question {
      override def progressStepsCount: Int = 3
      override def isBeingAnswered(questionProgressIndex: Int): Boolean = questionProgressIndex == 1
      override def onlyFirstGainsPoints: Boolean = true

      def maxTime: Duration = Duration.ofSeconds(3)
    }
  }
}
