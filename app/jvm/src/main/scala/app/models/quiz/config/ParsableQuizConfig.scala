package app.models.quiz.config

import java.util

case class ParsableQuizConfig(
    rounds: java.util.List[ParsableQuizConfig.Round],
) {
  def this() = this(null)
  def parse: QuizConfig = ???
}

object ParsableQuizConfig {
  case class Round(
      name: String,
      questions: java.util.List[ParsableQuizConfig.Question],
  ) {
    def this() = this(null, null)
    def parse: QuizConfig.Round = ???
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
        choices = new util.ArrayList(),
        pointsToGain = 1,
        maxTimeSeconds = 0,
        onlyFirstGainsPoints = false,
      )
      override def parse: QuizConfig.Question = ???
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
      override def parse: QuizConfig.Question = ???
    }
  }
}
