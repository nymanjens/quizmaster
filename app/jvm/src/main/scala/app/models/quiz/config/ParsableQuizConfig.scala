package app.models.quiz.config

import hydro.common.GuavaReplacement.Preconditions.checkNotNull

import scala.collection.JavaConverters._
import java.time.Duration

case class ParsableQuizConfig(
    rounds: java.util.List[ParsableQuizConfig.Round],
) {
  def this() = this(null)
  def parse: QuizConfig = QuizConfig(
    rounds = rounds.asScala.toVector.map(_.parse),
  )
}

object ParsableQuizConfig {
  case class Round(
      name: String,
      questions: java.util.List[ParsableQuizConfig.Question],
  ) {
    def this() = this(null, null)
    def parse: QuizConfig.Round = QuizConfig.Round(
      name = checkNotNull(name),
      questions = questions.asScala.toVector.map(_.parse),
    )
  }

  trait Question {
    def parse: QuizConfig.Question
  }
  object Question {
    case class Single(
        question: String,
        answer: String,
        choices: java.util.List[String],
        pointsToGain: Int,
        maxTimeSeconds: Int,
        onlyFirstGainsPoints: Boolean,
    ) extends Question {
      def this() = this(
        question = null,
        answer = null,
        choices = null,
        pointsToGain = 1,
        maxTimeSeconds = 0,
        onlyFirstGainsPoints = false,
      )
      override def parse: QuizConfig.Question = QuizConfig.Question.Single(
        question = checkNotNull(question),
        answer = checkNotNull(answer),
        choices = if (choices == null) None else Some(choices.asScala.toVector),
        pointsToGain = pointsToGain,
        maxTime = if (maxTimeSeconds == 0) None else Some(Duration.ofSeconds(maxTimeSeconds)),
        onlyFirstGainsPoints = onlyFirstGainsPoints,
      )
    }
    case class Double(
        verbalQuestion: String,
        verbalAnswer: String,
        textualQuestion: String,
        textualAnswer: String,
        textualChoices: java.util.List[String],
        pointsToGain: Int,
    ) extends Question {
      def this() = this(
        verbalQuestion = null,
        verbalAnswer = null,
        textualQuestion = null,
        textualAnswer = null,
        textualChoices = null,
        pointsToGain = 1,
      )
      override def parse: QuizConfig.Question = QuizConfig.Question.Double(
        verbalQuestion = checkNotNull(verbalQuestion),
        verbalAnswer = checkNotNull(verbalAnswer),
        textualQuestion = checkNotNull(textualQuestion),
        textualAnswer = checkNotNull(textualAnswer),
        textualChoices = checkNotNull(textualChoices.asScala.toVector),
        pointsToGain = pointsToGain,
      )
    }
  }
}
