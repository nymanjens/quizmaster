package app.models.quiz.config

import scala.collection.immutable.Seq
import app.models.quiz.config.QuizConfig.Round
import java.time.Duration

import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.common.CollectionUtils
import hydro.common.CollectionUtils.conditionalOption

case class QuizConfig(
    rounds: Seq[Round],
    title: Option[String],
    author: Option[String],
    masterSecret: String,
)

object QuizConfig {

  case class Round(
      name: String,
      questions: Seq[Question],
      expectedTime: Option[Duration] = None,
  )

  sealed trait Question {

    def pointsToGain: Int
    def pointsToGainOnFirstAnswer: Int
    def pointsToGainOnWrongAnswer: Int

    def onlyFirstGainsPoints: Boolean
    def showSingleAnswerButtonToTeams: Boolean

    def progressStepsCount(includeAnswers: Boolean): Int
    final def progressStepsCount(implicit quizState: QuizState): Int = {
      progressStepsCount(includeAnswers = quizState.generalQuizSettings.showAnswers)
    }
    final def maxProgressIndex(includeAnswers: Boolean): Int = {
      progressStepsCount(includeAnswers = includeAnswers) - 1
    }
    final def maxProgressIndex(implicit quizState: QuizState): Int = {
      maxProgressIndex(includeAnswers = quizState.generalQuizSettings.showAnswers)
    }
    def shouldShowTimer(questionProgressIndex: Int): Boolean
    def maxTime: Duration

    /** Returns true if it would make sense to add a QuizState.Submission for this question for this progressIndex. */
    def submissionAreOpen(questionProgressIndex: Int): Boolean
    def isMultipleChoice: Boolean
    def answerIsVisible(questionProgressIndex: Int): Boolean

    def textualQuestion: String
    def maybeTextualChoices: Option[Seq[String]]

    /**
      * Returns true if the given submission is correct according to configured answer.
      *
      * Always returns false if the given value is not scorable.
      */
    def isCorrectAnswer(submissionValue: SubmissionValue): Boolean
  }

  object Question {
    case class Single(
        question: String,
        questionDetail: Option[String],
        choices: Option[Seq[String]],
        answer: String,
        answerDetail: Option[String],
        answerImage: Option[Image],
        masterNotes: Option[String],
        image: Option[Image],
        // Relative path in audio directory
        audioSrc: Option[String],
        override val pointsToGain: Int,
        override val pointsToGainOnFirstAnswer: Int,
        override val pointsToGainOnWrongAnswer: Int,
        override val maxTime: Duration,
        override val onlyFirstGainsPoints: Boolean,
        override val showSingleAnswerButtonToTeams: Boolean,
    ) extends Question {
      def validationErrors(): Seq[String] = {
        choices match {
          case Some(choicesSeq) =>
            Seq(
              conditionalOption(choicesSeq.size != 4, s"Expected 4 choices, but got $choicesSeq"),
              conditionalOption(
                !(choicesSeq contains answer),
                s"The answer should be one of the choices: <<$answer>> not in <<$choicesSeq>>"),
            ).flatten
          case None => Seq()
        }
      }

      /**
        * Steps:
        * 0- Show preparatory title: "Question 2"
        * 1- Show question: "This is the question, do you know the answer?"
        * 2- Show answer
        * 3- (if possible) Show answer and give points
        */
      override def progressStepsCount(includeAnswers: Boolean): Int = {
        def oneIfTrue(b: Boolean): Int = if (b) 1 else 0
        val includeStep2 = includeAnswers
        val includeStep3 = includeAnswers && !showSingleAnswerButtonToTeams

        2 + oneIfTrue(includeStep2) + oneIfTrue(includeStep3)
      }
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }

      override def isMultipleChoice: Boolean = choices.nonEmpty
      override def textualQuestion: String = question
      override def maybeTextualChoices: Option[Seq[String]] = choices

      override def isCorrectAnswer(submissionValue: SubmissionValue): Boolean = {
        submissionValue match {
          case SubmissionValue.PressedTheOneButton               => false
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) => choices.get.apply(answerIndex) == answer
          case SubmissionValue.FreeTextAnswer(freeTextAnswer) =>
            def normalizeTextForComparison(s: String): String = {
              s.replace(" ", "").replace(".", "").replace("-", "").toLowerCase
            }
            normalizeTextForComparison(answer) == normalizeTextForComparison(freeTextAnswer)
        }
      }

      def questionIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
      def choicesAreVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
      override def answerIsVisible(questionProgressIndex: Int): Boolean = {
        if (!showSingleAnswerButtonToTeams) {
          questionProgressIndex >= maxProgressIndex(includeAnswers = true) - 1
        } else {
          questionProgressIndex == maxProgressIndex(includeAnswers = true)
        }
      }
    }

    case class Double(
        verbalQuestion: String,
        verbalAnswer: String,
        override val textualQuestion: String,
        textualAnswer: String,
        textualChoices: Seq[String],
        override val pointsToGain: Int,
    ) extends Question {
      def validationErrors(): Seq[String] = {
        Seq(
          conditionalOption(textualChoices.size != 4, s"Expected 4 choices, but got ${textualChoices}"),
          conditionalOption(
            !(textualChoices contains textualAnswer),
            s"The answer should be one of the choices: <<$textualAnswer>> not in <<${textualChoices}>>"),
        ).flatten
      }

      override def pointsToGainOnFirstAnswer: Int = pointsToGain
      override def pointsToGainOnWrongAnswer: Int = 0

      override def onlyFirstGainsPoints: Boolean = true
      override def showSingleAnswerButtonToTeams: Boolean = false

      /**
        * Steps:
        * 0- Show preparatory title: "Question 2"
        * 1- Show question: "This is the question, do you know the answer?"
        * 2- Show choices; when right answer is given, start the timer
        * 3- Show answer
        * 4- Show answer and give points
        */
      override def progressStepsCount(includeAnswers: Boolean): Int = {
        if (includeAnswers) 5 else 3
      }
      // Submissions should not be hindered by a timer
      override def shouldShowTimer(questionProgressIndex: Int): Boolean = false

      override def maxTime: Duration = Duration.ofSeconds(3)

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = questionProgressIndex == 2
      override def isMultipleChoice: Boolean = true
      override def maybeTextualChoices: Option[Seq[String]] = Some(textualChoices)

      override def isCorrectAnswer(submissionValue: SubmissionValue): Boolean = {
        (submissionValue: @unchecked) match {
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) =>
            textualChoices.apply(answerIndex) == textualAnswer
        }
      }

      def questionIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
      def choicesAreVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 2
      }
      override def answerIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= maxProgressIndex(includeAnswers = true) - 1
      }
    }
  }

  case class Image(
      // Relative path in image directory
      src: String,
      size: String,
  ) {
    def validationErrors(): Seq[String] = {
      Seq(
        conditionalOption(
          !(Seq("large", "small") contains size),
          s"size: '$size' should be either 'large' or 'small'"),
      ).flatten
    }
  }
}
