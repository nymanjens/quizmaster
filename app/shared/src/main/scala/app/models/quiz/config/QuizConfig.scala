package app.models.quiz.config

import java.time.Duration

import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.config.QuizConfig.UsageStatistics
import app.models.quiz.QuizState.Submission.SubmissionValue.MultipleTextAnswers
import hydro.common.CollectionUtils.conditionalOption
import hydro.common.GuavaReplacement.ImmutableBiMap
import hydro.common.GuavaReplacement.Splitter

import scala.collection.immutable.Seq
import scala.collection.mutable

case class QuizConfig(
    rounds: Seq[Round],
    title: Option[String],
    author: Option[String],
    instructionsOnFirstSlide: Option[String],
    masterSecret: String,
    languageCode: String,
    usageStatistics: UsageStatistics,
)
object QuizConfig {

  private def normalizeTextForComparison(s: String): String = {
    s.replace(" ", "").replace(".", "").replace("-", "").toLowerCase
  }

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
    def questionIsVisible(questionProgressIndex: Int): Boolean
    def maxTime: Duration

    /** Returns true if it would make sense to add a QuizState.Submission for this question for this progressIndex. */
    def submissionAreOpen(questionProgressIndex: Int): Boolean
    def isMultipleChoice: Boolean
    def answerIsVisible(questionProgressIndex: Int): Boolean

    def textualQuestion: String
    def questionDetail: Option[String]
    def masterNotes: Option[String]
    def tag: Option[String]
    def answerDetail: Option[String]
    def maybeTextualChoices: Option[Seq[String]]
    def answerAsString: String
    def image: Option[Image]
    def answerImage: Option[Image]
    def audioSrc: Option[String]
    def videoSrc: Option[String]

    /**
     * Returns true if the given submission is correct according to configured answer.
     *
     * Always returns false if the given value is not scorable.
     */
    def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean]
  }

  object Question {

    sealed trait Standard4StepQuestionBase extends Question {

      /**
       * Steps:
       * 0- Show preparatory title: "Question 2"
       * 1- Show question: "This is the question, do you know the answer?"
       * 2- Show answer
       * 3- (if possible) Show answer and give points
       */
      override final def progressStepsCount(includeAnswers: Boolean): Int = {
        def oneIfTrue(b: Boolean): Int = if (b) 1 else 0
        val includeStep2 = includeAnswers
        val includeStep3 = includeAnswers && !showSingleAnswerButtonToTeams

        2 + oneIfTrue(includeStep2) + oneIfTrue(includeStep3)
      }

      override final def shouldShowTimer(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }

      override final def submissionAreOpen(questionProgressIndex: Int): Boolean = {
        questionProgressIndex == 1
      }

      override final def questionIsVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }

      override final def answerIsVisible(questionProgressIndex: Int): Boolean = {
        if (showSingleAnswerButtonToTeams) {
          questionProgressIndex == maxProgressIndex(includeAnswers = true)
        } else {
          questionProgressIndex >= maxProgressIndex(includeAnswers = true) - 1
        }
      }
    }

    case class Standard(
        question: String,
        override val questionDetail: Option[String],
        override val tag: Option[String],
        choices: Option[Seq[String]],
        answer: String,
        override val answerDetail: Option[String],
        override val masterNotes: Option[String],
        override val image: Option[Image],
        override val answerImage: Option[Image],
        override val audioSrc: Option[String],
        override val videoSrc: Option[String],
        pointsToGain: FixedPointNumber,
        pointsToGainOnFirstAnswer: FixedPointNumber,
        pointsToGainOnWrongAnswer: FixedPointNumber,
        override val maxTime: Duration,
        override val onlyFirstGainsPoints: Boolean,
        override val showSingleAnswerButtonToTeams: Boolean,
    ) extends Standard4StepQuestionBase {

      def validationErrors(): Seq[String] = {
        choices match {
          case Some(choicesSeq) =>
            Seq(
              conditionalOption(choicesSeq.size < 2, s"Expected at least 2 choices, but got $choicesSeq"),
              conditionalOption(choicesSeq.size > 10, s"Expected at most 10 choices, but got $choicesSeq"),
              conditionalOption(
                !(choicesSeq contains answer),
                s"The answer should be one of the choices: <<$answer>> not in <<$choicesSeq>>",
              ),
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

      override def isMultipleChoice: Boolean = choices.nonEmpty
      override def textualQuestion: String = question
      override def maybeTextualChoices: Option[Seq[String]] = choices
      override def answerAsString: String = answer

      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        submissionValue match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) =>
            Some(choices.get.apply(answerIndex) == answer)
          case SubmissionValue.FreeTextAnswer(freeTextAnswer) =>
            Some(normalizeTextForComparison(answer) == normalizeTextForComparison(freeTextAnswer))
          case SubmissionValue.MultipleTextAnswers(_) => None
        }
      }

      def choicesAreVisible(questionProgressIndex: Int): Boolean = {
        questionProgressIndex >= 1
      }
    }

    sealed trait MultipleAnswersBase extends Question {}

    case class MultipleAnswers(
        question: String,
        override val questionDetail: Option[String],
        override val tag: Option[String],
        answers: Seq[String],
        answersHaveToBeInSameOrder: Boolean,
        override val answerDetail: Option[String],
        override val image: Option[Image],
        override val answerImage: Option[Image],
        override val audioSrc: Option[String],
        override val videoSrc: Option[String],
        pointsToGain: FixedPointNumber,
        override val maxTime: Duration,
    ) extends Standard4StepQuestionBase
        with MultipleAnswersBase {

      def validationErrors(): Seq[String] = {
        Seq(
          conditionalOption(answers.size < 2, s"Expected at least 2 answers, but got ${answers.size}")
        ).flatten
      }

      override def getPointsToGain(
          submissionValue: Option[SubmissionValue],
          isCorrect: Option[Boolean],
          previousCorrectSubmissionsExist: Boolean,
      ): FixedPointNumber = {
        isCorrect match {
          case None =>
            submissionValue match {
              case None                                          => FixedPointNumber(0)
              case Some(SubmissionValue.PressedTheOneButton)     => FixedPointNumber(0)
              case Some(SubmissionValue.MultipleChoiceAnswer(_)) => FixedPointNumber(0)
              case Some(SubmissionValue.FreeTextAnswer(_))       => FixedPointNumber(0)
              case Some(SubmissionValue.MultipleTextAnswers(answers)) =>
                val correctness = getCorrectnessPercentage(answers)
                if (correctness < 1 && pointsToGain * correctness == pointsToGain) {
                  // Ensure that non-perfect answers have at least 0.1 difference with correct answers
                  pointsToGain - FixedPointNumber(0.1)
                } else if (correctness > 0 && pointsToGain * correctness == FixedPointNumber(0)) {
                  // Ensure that non-perfect answers have at least 0.1 difference with incorrect answers
                  FixedPointNumber(0.1)
                } else {
                  pointsToGain * correctness
                }

            }
          case Some(true)  => pointsToGain
          case Some(false) => FixedPointNumber(0)
        }
      }

      private def getCorrectnessPercentage(answers: Seq[MultipleTextAnswers.Answer]): Double = {
        val correctAnswers = answers.count(_.isCorrectAnswer)
        correctAnswers * 1.0 / this.answers.size
      }

      override def onlyFirstGainsPoints: Boolean = false
      override def showSingleAnswerButtonToTeams: Boolean = false
      override def isMultipleChoice: Boolean = false
      override def textualQuestion: String = question
      override def masterNotes: Option[String] = None
      override def maybeTextualChoices: Option[Seq[String]] = None

      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        submissionValue match {
          case SubmissionValue.PressedTheOneButton     => None
          case SubmissionValue.MultipleChoiceAnswer(_) => None
          case SubmissionValue.FreeTextAnswer(_)       => None
          case SubmissionValue.MultipleTextAnswers(answers) =>
            if (getCorrectnessPercentage(answers) == 1.0) {
              Some(true)
            } else if (getCorrectnessPercentage(answers) == 0.0) {
              Some(false)
            } else {
              None
            }
        }
      }

      override def answerAsString: String = {
        answers.mkString(", ")
      }

      def createAutogradedAnswers(answerTexts: Seq[String]): Seq[MultipleTextAnswers.Answer] = {
        if (answersHaveToBeInSameOrder) {
          for ((correctAnswer, answerText) <- this.answers zip answerTexts)
            yield MultipleTextAnswers.Answer(
              text = answerText,
              isCorrectAnswer =
                normalizeTextForComparison(correctAnswer) == normalizeTextForComparison(answerText),
            )
        } else {
          val remainingNormalizedAnswers = mutable.Set(this.answers.map(normalizeTextForComparison): _*)
          for (answerText <- answerTexts) yield {
            val inAnswerPool = remainingNormalizedAnswers.remove(normalizeTextForComparison(answerText))
            MultipleTextAnswers.Answer(
              text = answerText,
              isCorrectAnswer = inAnswerPool,
            )
          }
        }
      }
    }

//    case class MultipleQuestions() extends Standard4StepQuestionBase        with MultipleAnswersBase {

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
            s"The answer should be one of the choices: <<$textualAnswer>> not in <<${textualChoices}>>",
          ),
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
      override def questionDetail: Option[String] = None
      override def masterNotes: Option[String] = None
      override def tag: Option[String] = None
      override def answerDetail: Option[String] = None
      override def maybeTextualChoices: Option[Seq[String]] = Some(textualChoices)
      override def answerAsString: String = textualAnswer
      override def image: Option[Image] = None
      override def answerImage: Option[Image] = None
      override def audioSrc: Option[String] = None
      override def videoSrc: Option[String] = None

      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        (submissionValue: @unchecked) match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.MultipleChoiceAnswer(answerIndex) =>
            Some(textualChoices.apply(answerIndex) == textualAnswer)
        }
      }

      override def questionIsVisible(questionProgressIndex: Int): Boolean = {
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
        override val questionDetail: Option[String],
        override val tag: Option[String],
        orderedItemsThatWillBePresentedInAlphabeticalOrder: Seq[OrderItems.Item],
        override val answerDetail: Option[String],
        pointsToGain: FixedPointNumber,
        override val maxTime: Duration,
    ) extends Standard4StepQuestionBase {
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
              case Some(SubmissionValue.FreeTextAnswer(a)) =>
                val correctness = getCorrectnessPercentage(a)
                if (correctness < 1 && pointsToGain * correctness == pointsToGain) {
                  // Ensure that non-perfect answers have at least 0.1 difference with correct answers
                  pointsToGain - FixedPointNumber(0.1)
                } else {
                  pointsToGain * correctness
                }

            }
          case Some(true)  => pointsToGain
          case Some(false) => FixedPointNumber(0)
        }
      }

      private def getCorrectnessPercentage(answer: String): Double = {
        val N = itemsInAlphabeticalOrder.size
        val maxNumberOfPairwiseSwaps = ((N - 1) * N) / 2
        val charactersInCorrectOrder = answerAsString

        if (!isValidAnswerString(answer)) {
          0.0 // Return early because the answer cannot be parsed
        } else {
          var numSwaps = 0
          var remainingAnswerString = answer

          for (char <- charactersInCorrectOrder) {
            numSwaps += remainingAnswerString.indexOf(char)
            remainingAnswerString = remainingAnswerString.filter(_ != char)
          }

          require(numSwaps <= maxNumberOfPairwiseSwaps)

          1 - (numSwaps * 1.0 / maxNumberOfPairwiseSwaps)
        }
      }

      override def onlyFirstGainsPoints: Boolean = false
      override def showSingleAnswerButtonToTeams: Boolean = false

      override def isMultipleChoice: Boolean = false

      override def textualQuestion: String = question
      override def masterNotes: Option[String] = None
      override def maybeTextualChoices: Option[Seq[String]] = None
      override def isCorrectAnswer(submissionValue: SubmissionValue): Option[Boolean] = {
        (submissionValue: @unchecked) match {
          case SubmissionValue.PressedTheOneButton => None
          case SubmissionValue.FreeTextAnswer(a) =>
            if (getCorrectnessPercentage(a) == 1.0) {
              Some(true)
            } else if (getCorrectnessPercentage(a) == 0.0) {
              Some(false)
            } else {
              None
            }
        }
      }

      override def answerAsString: String = {
        orderedItemsThatWillBePresentedInAlphabeticalOrder.map(i => toCharacterCode(i)).mkString
      }
      override def image: Option[Image] = None
      override def answerImage: Option[Image] = None
      override def audioSrc: Option[String] = None
      override def videoSrc: Option[String] = None

      lazy val itemsInAlphabeticalOrder: Seq[OrderItems.Item] = {
        orderedItemsThatWillBePresentedInAlphabeticalOrder.sortBy(_.item)
      }

      private lazy val itemToCharacterBimap: ImmutableBiMap[OrderItems.Item, Char] = {
        val resultBuilder = ImmutableBiMap.builder[OrderItems.Item, Char]()
        val usedCharacters = mutable.Set[Char]()

        for (item <- itemsInAlphabeticalOrder) {
          val words = Splitter.on(' ').trimResults().omitEmptyStrings().split(item.item)
          val candidatesFromWords = words.map(_.apply(0)).filter(_.isLetterOrDigit).map(_.toUpper)
          val candidates = candidatesFromWords ++ "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          val char = candidates.find(c => !usedCharacters.contains(c)).get

          usedCharacters.add(char)
          resultBuilder.put(item, char)
        }

        resultBuilder.build
      }

      def toCharacterCode(item: OrderItems.Item): Char = {
        itemToCharacterBimap.get(item)
      }

      def itemFromCharacterCode(char: Char): OrderItems.Item = {
        itemToCharacterBimap.inverse().get(char)
      }

      def isValidAnswerString(answer: String): Boolean = {
        answer.length == itemsInAlphabeticalOrder.size &&
        answer.toSet == itemToCharacterBimap.inverse().keySet
      }
    }
    object OrderItems {
      case class Item(
          item: String,
          answerDetail: Option[String],
      )
    }
  }

  case class Image(
      src: String,
      size: String,
  ) {
    def validationErrors(): Seq[String] = {
      Seq(
        conditionalOption(
          !(Seq("large", "small") contains size),
          s"size: '$size' should be either 'large' or 'small'",
        )
      ).flatten
    }
  }

  case class UsageStatistics(
      sendAnonymousUsageDataAtEndOfQuiz: Boolean,
      includeAuthor: Boolean,
      includeQuizTitle: Boolean,
  )
  object UsageStatistics {
    val default: UsageStatistics = UsageStatistics(
      sendAnonymousUsageDataAtEndOfQuiz = false,
      includeAuthor = false,
      includeQuizTitle = false,
    )
  }
}
