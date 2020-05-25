package app.models.quiz.config

import java.time.Duration

import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.BooleanValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.IntValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ListParsableValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Optional
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Required
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.StringValue
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import hydro.common.GuavaReplacement.Preconditions.checkNotNull

import scala.collection.immutable.Seq

object QuizConfigParsableValue extends MapParsableValue[QuizConfig] {
  override val supportedKeyValuePairs = Map(
    "title" -> Optional(StringValue(defaultValue = null)),
    "author" -> Optional(StringValue(defaultValue = null)),
    "masterSecret" -> Optional(StringValue(defaultValue = "*")),
    "rounds" -> Required(ListParsableValue(RoundValue)),
  )

  override def parseFromParsedMapValues(map: Map[String, Any]) = {
    ParseResult(
      QuizConfig(
        rounds = checkNotNull(map("rounds").asInstanceOf[Seq[Round]]),
        title = Option(map("title").asInstanceOf[String]),
        author = Option(map("author").asInstanceOf[String]),
        masterSecret = checkNotNull(map("masterSecret").asInstanceOf[String]),
      )
    )
  }

  private object RoundValue extends MapParsableValue[Round] {
    override val supportedKeyValuePairs = Map(
      "name" -> Required(StringValue()),
      "questions" -> Required(ListParsableValue(QuestionValue)),
      "expectedTimeMinutes" -> Optional(IntValue(defaultValue = -1)),
    )
    override def parseFromParsedMapValues(map: Map[String, Any]) = {
      ParseResult(
        Round(
          name = checkNotNull(map("name").asInstanceOf[String]),
          questions = checkNotNull(map("questions").asInstanceOf[Seq[Question]]),
          expectedTime =
            if (map("expectedTimeMinutes") == -1) None
            else Some(Duration.ofMinutes(map("expectedTimeMinutes").asInstanceOf[Int])),
        )
      )
    }
  }

  private object QuestionValue extends MapParsableValue[Question] {
    override val supportedKeyValuePairs = Map(
      "type" -> Optional(StringValue(defaultValue = "single")),
      "question" -> Required(StringValue()),
      "questionDetail" -> Optional(StringValue(defaultValue = null)),
      "choices" -> Optional(ListParsableValue(StringValue())),
      "answer" -> Required(StringValue()),
      "answerDetail" -> Optional(StringValue()),
      "answerImage" -> Optional(ImageValue),
      "masterNotes" -> Optional(StringValue()),
      "image" -> Optional(ImageValue),
      "audioSrc" -> Optional(StringValue(defaultValue = null)),
      "pointsToGain" -> Optional(IntValue(defaultValue = 1)),
      "pointsToGainOnFirstAnswer" -> Optional(IntValue(defaultValue = -9090)),
      "pointsToGainOnWrongAnswer" -> Optional(IntValue(defaultValue = 0)),
      "maxTimeSeconds" -> Required(IntValue()),
      "onlyFirstGainsPoints" -> Optional(BooleanValue()),
      "showSingleAnswerButtonToTeams" -> Optional(BooleanValue()),
    )
    override def parseFromParsedMapValues(map: Map[String, Any]) = {
      ParseResult(
        QuizConfig.Question.Single(
          question = checkNotNull(map("question").asInstanceOf[String]),
          questionDetail = Option(map("questionDetail").asInstanceOf[String]),
          choices = checkNotNull(map("choices").asInstanceOf[Seq[String]]) match {
            case Seq() => None
            case s     => Some(s)
          },
          answer = checkNotNull(map("answer").asInstanceOf[String]),
          answerDetail = Option(map("answerDetail").asInstanceOf[String]),
          answerImage = Option(map("answerImage").asInstanceOf[Image]),
          masterNotes = Option(map("masterNotes").asInstanceOf[String]),
          image = Option(map("image").asInstanceOf[Image]),
          audioSrc = Option(map("audioSrc").asInstanceOf[String]),
          pointsToGain = map("pointsToGain").asInstanceOf[Int],
          pointsToGainOnFirstAnswer = (if (map("pointsToGainOnFirstAnswer") == -9090) map("pointsToGain")
                                       else map("pointsToGainOnFirstAnswer")).asInstanceOf[Int],
          pointsToGainOnWrongAnswer = map("pointsToGainOnWrongAnswer").asInstanceOf[Int],
          maxTime = Duration.ofSeconds(map("maxTimeSeconds").asInstanceOf[Int]),
          onlyFirstGainsPoints = map("onlyFirstGainsPoints").asInstanceOf[Boolean],
          showSingleAnswerButtonToTeams = map("showSingleAnswerButtonToTeams").asInstanceOf[Boolean],
        )
      )
    }
  }

  private object ImageValue extends MapParsableValue[Image] {
    override val supportedKeyValuePairs = Map(
      "src" -> Required(StringValue()),
      "size" -> Optional(StringValue(defaultValue = "large")),
    )
    override def parseFromParsedMapValues(map: Map[String, Any]) = {
      ParseResult(
        Image(
          src = checkNotNull(map("src").asInstanceOf[String]),
          size = checkNotNull(map("size").asInstanceOf[String]),
        )
      )
    }
  }
}
