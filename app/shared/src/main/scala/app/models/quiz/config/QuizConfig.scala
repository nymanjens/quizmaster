package app.models.quiz.config

import java.time.Duration

import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.common.CollectionUtils.conditionalOption
import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.GuavaReplacement.Splitter

import scala.collection.immutable.Seq
import scala.collection.mutable

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

    def getPointsToGain(
        submissionValue: Option[SubmissionValue],
        isCorrect: Option[Boolean],
        previousCorrectSubmissionsExist: Boolean,
    ): FixedPointNumber
    final def defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer: Boolean): FixedPointNumber = {
      getPointsToGain(
        submissionValue = None,
        isCorrect = Some(true),
        previousCorrectSubmissionsExist = !isFirstCorrectAnswer,
      )
    }
    final def defaultPointsToGainOnWrongAnswer: FixedPointNumber = {
      getPointsToGain(
        submissionValue = None,
        isCorrect = Some(false),
        previousCorrectSubmissionsExist = false,
      )
    }

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
    def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean]
  }

  object Question {
    case class Standard(
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
        // Relative path in video directory
        videoSrc: Option[String],
        pointsToGain: FixedPointNumber,
        pointsToGainOnFirstAnswer: FixedPointNumber,
        pointsToGainOnWrongAnswer: FixedPointNumber,
        override val maxTime: Duration,
        override val onlyFirstGainsPoints: Boolean,
        override val showSingleAnswerButtonToTeams: Boolean,
    ) extends Question {

      def validationErrors(): Seq[String] = {
        choices match {
          case Some(choicesSeq) =>
            Seq(
              conditionalOption(choicesSeq.size < 2, s"Expected at least 2 choices, but got $choicesSeq"),
              conditionalOption(choicesSeq.size > 10, s"Expected at most 10 choices, but got $choicesSeq"),
              conditionalOption(
                !(choicesSeq contains answer),
                s"The answer should be one of the choices: <<$answer>> not in <<$choicesSeq>>"),
            ).flatten
          case None => Seq()
        }
      }

      override def getPointsToGain(
          submissionValue: Option[SubmissionValue],
          isCorrect: Option[Boolean],
          previousCorrectSubmissionsExist: Boolean,
      ): FixedPointNumber = {
        isCorrect match {
          case None                                           => FixedPointNumber(0)
          case Some(true) if !previousCorrectSubmissionsExist => pointsToGainOnFirstAnswer
          case Some(true) if previousCorrectSubmissionsExist  => pointsToGain
          case Some(false)                                    => pointsToGainOnWrongAnswer
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

      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        submissionValue match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) =>
            Some(choices.get.apply(answerIndex) == answer)
          case SubmissionValue.FreeTextAnswer(freeTextAnswer) =>
            def normalizeTextForComparison(s: String): String = {
              s.replace(" ", "").replace(".", "").replace("-", "").toLowerCase
            }
            Some(normalizeTextForComparison(answer) == normalizeTextForComparison(freeTextAnswer))
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

    // This cannot be "Double" because that conflicts with the scala native type
    case class DoubleQ(
        verbalQuestion: String,
        verbalAnswer: String,
        override val textualQuestion: String,
        textualAnswer: String,
        textualChoices: Seq[String],
        pointsToGain: FixedPointNumber,
    ) extends Question {

      def validationErrors(): Seq[String] = {
        Seq(
          conditionalOption(textualChoices.size != 4, s"Expected 4 choices, but got ${textualChoices}"),
          conditionalOption(
            !(textualChoices contains textualAnswer),
            s"The answer should be one of the choices: <<$textualAnswer>> not in <<${textualChoices}>>"),
        ).flatten
      }

      override def getPointsToGain(
          submissionValue: Option[SubmissionValue],
          isCorrect: Option[Boolean],
          previousCorrectSubmissionsExist: Boolean,
      ): FixedPointNumber = {
        isCorrect match {
          case None        => FixedPointNumber(0)
          case Some(true)  => pointsToGain
          case Some(false) => FixedPointNumber(0)
        }
      }

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

      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        (submissionValue: @unchecked) match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) =>
            Some(textualChoices.apply(answerIndex) == textualAnswer)
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

    case class OrderItems(
        question: String,
        questionDetail: Option[String],
        orderedItemsThatWillBePresentedInAlphabeticalOrder: Seq[String],
        pointsToGain: FixedPointNumber,
        override val maxTime: Duration,
    ) extends Question {
      def validationErrors(): Seq[String] = {
        val orderedItems = orderedItemsThatWillBePresentedInAlphabeticalOrder
        Seq(
          conditionalOption(orderedItems.size < 2, s"Expected at least 2 items, but got $orderedItems"),
          conditionalOption(orderedItems.size > 10, s"Expected at most 10 items, but got $orderedItems"),
        ).flatten
      }

      override def getPointsToGain(
          submissionValue: Option[SubmissionValue],
          isCorrect: Option[Boolean],
          previousCorrectSubmissionsExist: Boolean,
      ): FixedPointNumber = {
        isCorrect match {
          case None =>
            (submissionValue: @unchecked) match {
              case None                                      => FixedPointNumber(0)
              case Some(SubmissionValue.PressedTheOneButton) => FixedPointNumber(0)
              case Some(SubmissionValue.FreeTextAnswer(a))   => pointsToGain * getCorrectnessPercentage(a)

            }
          case Some(true)  => pointsToGain
          case Some(false) => FixedPointNumber(0)
        }
      }

      private def getCorrectnessPercentage(answer: String): Double = {
        val N = itemsInAlphabeticalOrder.size
        val maxNumberOfPairwiseSwaps = ((N - 1) * N) / 2
          val charactersInCorrectOrder = answerAsString

        if(answer.length != N || answer.toSet != itemToCharacterBimap.inverse().keySet) {
          0.0 // Return early because the answer cannot be parsed
        } else {
          var numSwaps = 0
          var remainingAnswerString = answer

          for(char <- charactersInCorrectOrder) {
            numSwaps += remainingAnswerString.indexOf(char)
            remainingAnswerString = remainingAnswerString.filter(_ != char)
          }

          require(numSwaps <= maxNumberOfPairwiseSwaps)

          1 - (numSwaps * 1.0 / maxNumberOfPairwiseSwaps)
        }
      }

      override def onlyFirstGainsPoints: Boolean = false
      override def showSingleAnswerButtonToTeams: Boolean = false

      /**
        * Steps:
        * 0- Show preparatory title: "Question 2"
        * 1- Show question: "This is the question, do you know the answer?"
        * 2- Show answer
        * 3- Show answer and give points
        */
      override def progressStepsCount(includeAnswers: Boolean): Int = {
        if (includeAnswers) 2 else 4
      }

      override def shouldShowTimer(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }

      override def submissionAreOpen(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }
      override def isMultipleChoice: Boolean = false

      override def answerIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= maxProgressIndex(includeAnswers = true) - 1
      }
      override def textualQuestion: String = question
      override def maybeTextualChoices: Option[Seq[String]] = None
      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        (submissionValue: @unchecked) match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.FreeTextAnswer(a) =>
            if (a == answerAsString) {
              Some(true)
            } else if (a == answerAsString.reverse) {
              Some(false)
            } else {
              None
            }
        }
      }

      def answerAsString: String = {
        orderedItemsThatWillBePresentedInAlphabeticalOrder.map(toCharacterCode).mkString
      }

      lazy val itemsInAlphabeticalOrder: Seq[String] = {
        orderedItemsThatWillBePresentedInAlphabeticalOrder.sorted
      }

      private lazy val itemToCharacterBimap: ImmutableBiMap[String, Char] = {
        val resultBuilder = ImmutableBiMap.builder[String, Char]()

        for (item <- itemsInAlphabeticalOrder) {
          val usedCharacters = mutable.Set[Char]()
          val words = Splitter.on(' ').trimResults().omitEmptyStrings().split(item)
          val candidatesFromWords = words.map(_.apply(0)).filter(_.isLetterOrDigit).map(_.toUpper)
          val candidates = candidatesFromWords ++ "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          val char = candidates.find(c => !usedCharacters.contains(c)).get

          usedCharacters.add(char)
          resultBuilder.put(item, char)
        }

        resultBuilder.build
      }

      def toCharacterCode(item: String): Char = {
        itemToCharacterBimap.get(item)
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
