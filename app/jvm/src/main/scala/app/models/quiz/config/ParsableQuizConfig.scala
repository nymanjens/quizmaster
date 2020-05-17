package app.models.quiz.config

import hydro.common.GuavaReplacement.Preconditions.checkNotNull

import scala.collection.JavaConverters._
import java.time.Duration

case class ParsableQuizConfig(
    rounds: java.util.List[ParsableQuizConfig.Round],
    title: String,
    author: String,
    masterSecret: String,
) {
  def this() = this(
    rounds = null,
    title = null,
    author = null,
    masterSecret = "*",
  )
  def parse: QuizConfig = {
    try {
      QuizConfig(
        rounds = rounds.asScala.toVector.map(_.parse),
        title = Option(title),
        author = Option(author),
        masterSecret = checkNotNull(masterSecret),
      )
    } catch {
      case throwable: Throwable =>
        throw new RuntimeException(s"Failed to parse QuizConfig", throwable)
    }
  }
}

object ParsableQuizConfig {
  case class Round(
      name: String,
      questions: java.util.List[ParsableQuizConfig.Question],
      expectedTimeMinutes: Int,
  ) {
    def this() = this(null, null, expectedTimeMinutes = -1)
    def parse: QuizConfig.Round = {
      try {
        QuizConfig.Round(
          name = checkNotNull(name),
          questions = questions.asScala.toVector.map(_.parse),
          expectedTime =
            if (expectedTimeMinutes == -1) None else Some(Duration.ofMinutes(expectedTimeMinutes)),
        )
      } catch {
        case throwable: Throwable =>
          throw new RuntimeException(s"Failed to parse Round $name", throwable)
      }
    }
  }

  trait Question {
    def parse: QuizConfig.Question
  }
  object Question {
    case class Single(
        question: String,
        questionDetail: String,
        choices: java.util.List[String],
        answer: String,
        answerDetail: String,
        answerImage: Image,
        masterNotes: String,
        image: Image,
        audioSrc: String,
        pointsToGain: Int,
        pointsToGainOnFirstAnswer: java.lang.Integer,
        pointsToGainOnWrongAnswer: Int,
        maxTimeSeconds: java.lang.Integer,
        onlyFirstGainsPoints: Boolean,
        showSingleAnswerButtonToTeams: Boolean,
    ) extends Question {
      def this() = this(
        question = null,
        questionDetail = null,
        choices = null,
        answer = null,
        answerDetail = null,
        answerImage = null,
        masterNotes = null,
        image = null,
        audioSrc = null,
        pointsToGain = 1,
        pointsToGainOnFirstAnswer = null,
        pointsToGainOnWrongAnswer = 0,
        maxTimeSeconds = null,
        onlyFirstGainsPoints = false,
        showSingleAnswerButtonToTeams = false,
      )
      override def parse: QuizConfig.Question = {
        try {
          require(maxTimeSeconds != null, "maxTimeSeconds is not set")
          QuizConfig.Question.Single(
            question = checkNotNull(question),
            questionDetail = Option(questionDetail),
            choices = if (choices == null) None else Some(choices.asScala.toVector),
            answer = checkNotNull(answer),
            answerDetail = Option(answerDetail),
            answerImage = Option(answerImage).map(_.parse),
            masterNotes = Option(masterNotes),
            image = Option(image).map(_.parse),
            audioSrc = Option(audioSrc),
            pointsToGain = pointsToGain,
            pointsToGainOnFirstAnswer = Option(pointsToGainOnFirstAnswer).map(_.toInt) getOrElse pointsToGain,
            pointsToGainOnWrongAnswer = pointsToGainOnWrongAnswer,
            maxTime = Duration.ofSeconds(maxTimeSeconds.toInt),
            onlyFirstGainsPoints = onlyFirstGainsPoints,
            showSingleAnswerButtonToTeams = showSingleAnswerButtonToTeams,
          )
        } catch {
          case throwable: Throwable =>
            throw new RuntimeException(s"Failed to parse Question.Single $question", throwable)
        }
      }
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
        pointsToGain = 2,
      )
      override def parse: QuizConfig.Question = {
        try {
          QuizConfig.Question.Double(
            verbalQuestion = checkNotNull(verbalQuestion),
            verbalAnswer = checkNotNull(verbalAnswer),
            textualQuestion = checkNotNull(textualQuestion),
            textualAnswer = checkNotNull(textualAnswer),
            textualChoices = checkNotNull(textualChoices.asScala.toVector),
            pointsToGain = pointsToGain,
          )
        } catch {
          case throwable: Throwable =>
            throw new RuntimeException(s"Failed to parse Question.Double $verbalQuestion", throwable)
        }
      }
    }
  }

  case class Image(src: String, size: String) {
    def this() = this(
      src = null,
      size = "large",
    )

    def parse: QuizConfig.Image = {
      QuizConfig.Image(
        src = checkNotNull(src),
        size = checkNotNull(size),
      )
    }
  }
}
