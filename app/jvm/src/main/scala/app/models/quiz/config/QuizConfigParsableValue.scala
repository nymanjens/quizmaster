package app.models.quiz.config

import java.time.Duration

import scala.collection.JavaConverters._
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.config.ValidatingYamlParser.ParsableValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.BooleanValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.IntValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ListParsableValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Optional
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Required
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.StringMap
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.StringValue
import app.models.quiz.config.ValidatingYamlParser.ParseResult

import scala.collection.immutable.Seq

object QuizConfigParsableValue extends MapParsableValue[QuizConfig] {
  override val supportedKeyValuePairs = Map(
    "title" -> Optional(StringValue),
    "author" -> Optional(StringValue),
    "masterSecret" -> Optional(StringValue),
    "rounds" -> Required(ListParsableValue(RoundValue)(_.name)),
  )

  override def parseFromParsedMapValues(map: StringMap) = {
    ParseResult.success(
      QuizConfig(
        title = map.optional("title"),
        author = map.optional("author"),
        masterSecret = map.optional("masterSecret", "*"),
        rounds = map.required[Seq[Round]]("rounds"),
      )
    )
  }

  private object RoundValue extends MapParsableValue[Round] {
    override val supportedKeyValuePairs = Map(
      "name" -> Required(StringValue),
      "questions" -> Required(ListParsableValue(QuestionValue)(_.textualQuestion)),
      "expectedTimeMinutes" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      ParseResult.success(
        Round(
          name = map.required[String]("name"),
          questions = map.required[Seq[Question]]("questions"),
          expectedTime = map.optional("expectedTimeMinutes").map(Duration.ofMinutes),
        )
      )
    }
  }

  private object QuestionValue extends ParsableValue[Question] {
    override def parse(yamlValue: Any): ParseResult[Question] = {
      if (yamlValue.isInstanceOf[java.util.Map[_, _]]) {
        val yamlMap = yamlValue.asInstanceOf[java.util.Map[String, _]].asScala
        val yamlMapWithoutQuestionType = (yamlMap - "questionType").asJava

        yamlMap.get("questionType") match {
          case None | Some("standard") => StandardQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some("double")          => DoubleQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some(other) =>
            ParseResult.onlyError(
              s"questionType expected to be one of these: [unset, 'standard', 'double'], but found $other")
        }

      } else {
        ParseResult.onlyError(s"Expected a map but found $yamlValue")
      }
    }
  }

  private object StandardQuestionValue extends MapParsableValue[Question] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "choices" -> Optional(ListParsableValue(StringValue)(s => s)),
      "answer" -> Required(StringValue),
      "answerDetail" -> Optional(StringValue),
      "answerImage" -> Optional(ImageValue),
      "masterNotes" -> Optional(StringValue),
      "image" -> Optional(ImageValue),
      "audioSrc" -> Optional(StringValue),
      "pointsToGain" -> Optional(IntValue),
      "pointsToGainOnFirstAnswer" -> Optional(IntValue),
      "pointsToGainOnWrongAnswer" -> Optional(IntValue),
      "maxTimeSeconds" -> Required(IntValue),
      "onlyFirstGainsPoints" -> Optional(BooleanValue),
      "showSingleAnswerButtonToTeams" -> Optional(BooleanValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      ParseResult.success(
        Question.Single(
          question = map.required[String]("question"),
          questionDetail = map.optional("questionDetail"),
          choices = map.optional("choices"),
          answer = map.required[String]("answer"),
          answerDetail = map.optional("answerDetail"),
          answerImage = map.optional("answerImage"),
          masterNotes = map.optional("masterNotes"),
          image = map.optional("image"),
          audioSrc = map.optional("audioSrc"),
          pointsToGain = map.optional("pointsToGain", 1),
          pointsToGainOnFirstAnswer =
            map.optional("pointsToGainOnFirstAnswer") getOrElse map.optional("pointsToGain", 1),
          pointsToGainOnWrongAnswer = map.optional("pointsToGainOnWrongAnswer", 0),
          maxTime = Duration.ofSeconds(map.required[Int]("maxTimeSeconds")),
          onlyFirstGainsPoints = map.optional("onlyFirstGainsPoints", false),
          showSingleAnswerButtonToTeams = map.optional("showSingleAnswerButtonToTeams", false),
        )
      )
    }
  }


  private object DoubleQuestionValue extends MapParsableValue[Question] {
    override val supportedKeyValuePairs = Map(
      "verbalQuestion" -> Required(StringValue),
      "verbalAnswer" -> Required(StringValue),
      "textualQuestion" -> Required(StringValue),
      "textualAnswer" -> Required(StringValue),
      "textualChoices" -> Required(ListParsableValue(StringValue)(s => s)),
      "pointsToGain" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      ParseResult.success(
        Question.Double(
            verbalQuestion = map.required[String]("verbalQuestion"),
            verbalAnswer = map.required[String]("verbalAnswer"),
            textualQuestion = map.required[String]("textualQuestion"),
            textualAnswer = map.required[String]("textualAnswer"),
            textualChoices = map.required[Seq[String]]("textualChoices"),
            pointsToGain = map.optional("pointsToGain", 2),
          )
      )
    }
  }

  private object ImageValue extends MapParsableValue[Image] {
    override val supportedKeyValuePairs = Map(
      "src" -> Required(StringValue),
      "size" -> Optional(StringValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      ParseResult.success(
        Image(
          src = map.required[String]("src"),
          size = map.optional("size", "large"),
        )
      )
    }
  }
}
