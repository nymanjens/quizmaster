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
    def pointsToGain: Int
    def pointsToGainOnWrongAnswer: Int

    def onlyFirstGainsPoints: Boolean

    def progressStepsCount: Int
    final def maxProgressIndex: Int = progressStepsCount - 1
    def shouldShowTimer(questionProgressIndex: Int): Boolean
    def maxTime: Option[Duration]

    /** Returns true if it would make sense to add a QuizState.Submission for this question for this progressIndex. */
    def submissionAreOpen(questionProgressIndex: Int): Boolean
  }

  object Question {
    case class Single(
        question: String,
        answer: String,
        choices: Option[Seq[String]],
        override val pointsToGain: Int,
        override val maxTime: Option[Duration],
        override val onlyFirstGainsPoints: Boolean,
    ) extends Question {

      override def pointsToGainOnWrongAnswer: Int = {
        if (onlyFirstGainsPoints && choices.nonEmpty) -pointsToGain else 0
      }

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
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = {
        maxTime.isDefined && questionProgressIndex == progressIndexForQuestionBeingAnswered
      }

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = {
        val rightProgressIndex = questionProgressIndex == progressIndexForQuestionBeingAnswered
        //val questionSupportsSubmissions = choices.nonEmpty || onlyFirstGainsPoints
        val questionSupportsSubmissions = true

        rightProgressIndex && questionSupportsSubmissions
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

      private def progressIndexForQuestionBeingAnswered: Int = {
        if (choices.isDefined) 2 else 1
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
      override def pointsToGainOnWrongAnswer: Int = -pointsToGain

      override def onlyFirstGainsPoints: Boolean = true

      override def progressStepsCount: Int = 3
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = questionProgressIndex == 2

      override def maxTime: Option[Duration] = Some(Duration.ofSeconds(3))

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = ???
    }
  }
}
