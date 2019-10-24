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
    def maybeMaxTime: Option[Duration]

    /** Returns true if it would make sense to add a QuizState.Submission for this question for this progressIndex. */
    def submissionAreOpen(questionProgressIndex: Int): Boolean
    def isMultipleChoice: Boolean

    /** If `isMultipleChoice` is true, this is a way to see if an answer is correct. */
    def isCorrectAnswerIndex(answerIndex: Int): Boolean
  }

  object Question {
    case class Single(
        question: String,
        answer: String,
        image: Option[String],
        choices: Option[Seq[String]],
        override val pointsToGain: Int,
        override val pointsToGainOnWrongAnswer: Int,
        override val maybeMaxTime: Option[Duration],
        override val onlyFirstGainsPoints: Boolean,
    ) extends Question {
      if (choices.isDefined) {
        require(choices.get.size == 4, s"There should be 4 choices, but got ${choices.get}")
      }

      /**
        * Steps:
        * 0- Show preparatory title: "Question 2"
        * 1- Show question: "This is the question, do you know the answer?"
        * 2- (if relevant) Show choices
        * 3- Show answer
        * 4- (if possible) Show answer and give points
        */
      override def progressStepsCount: Int = {
        if (choices.isDefined) 5 else 3
      }
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = {
        maybeMaxTime.isDefined && questionProgressIndex == progressIndexForQuestionBeingAnswered
      }

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = {
        val rightProgressIndex = questionProgressIndex == progressIndexForQuestionBeingAnswered
        //val questionSupportsSubmissions = choices.nonEmpty || onlyFirstGainsPoints
        val questionSupportsSubmissions = true

        rightProgressIndex && questionSupportsSubmissions
      }

      override def isMultipleChoice: Boolean = choices.nonEmpty
      override def isCorrectAnswerIndex(answerIndex: Int): Boolean = choices.get.apply(answerIndex) == answer

      def questionIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
      def choicesAreVisible(questionProgressIndex: Int): Boolean = {
        choices.isDefined && questionProgressIndex >= 2
      }
      def answerIsVisible(questionProgressIndex: Int): Boolean = {
        if (choices.isDefined) {
          questionProgressIndex >= maxProgressIndex - 1
        } else {
          questionProgressIndex == maxProgressIndex
        }
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
    ) extends Question {
      require(textualChoices.size == 4, s"Expected 4 choices, but got ${textualChoices}")

      override def pointsToGain: Int = 1
      override def pointsToGainOnWrongAnswer: Int = -1

      override def onlyFirstGainsPoints: Boolean = true

      override def progressStepsCount: Int = 3
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = questionProgressIndex == 2

      override def maybeMaxTime: Option[Duration] = Some(Duration.ofSeconds(3))

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = ???
      override def isMultipleChoice: Boolean = true
      override def isCorrectAnswerIndex(answerIndex: Int): Boolean =
        textualChoices.apply(answerIndex) == textualAnswer
    }
  }
}
