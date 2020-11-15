package app.models.quiz.config

import java.time.Duration

import app.common.FixedPointNumber
import app.common.QuizAssets

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
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.WithStringSimplification
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import com.google.inject.Inject
import hydro.common.I18n

import scala.collection.immutable.Seq

class QuizConfigParsableValue @Inject() (implicit
    quizAssets: QuizAssets,
    i18n: I18n,
) extends MapParsableValue[QuizConfig] {
  override val supportedKeyValuePairs = Map(
    "title" -> Optional(StringValue),
    "author" -> Optional(StringValue),
    "masterSecret" -> Optional(StringValue),
    "rounds" -> Required(ListParsableValue(RoundValue)(_.name)),
    "zipRoundsWithGeneticRoundNames" -> Optional(BooleanValue),
  )

  override def parseFromParsedMapValues(map: StringMap) = {
    QuizConfig(
      title = map.optional("title"),
      author = map.optional("author"),
      masterSecret = map.optional("masterSecret", "*"),
      rounds = {
        val rounds = map.required[Seq[Round]]("rounds")
        if (map.optional("zipRoundsWithGeneticRoundNames", false)) zipRoundsWithGeneticRoundNames(rounds)
        else rounds
      },
    )
  }

  private def zipRoundsWithGeneticRoundNames(rounds: Seq[Round]): Seq[Round] = {
    if (rounds.isEmpty) {
      rounds
    } else {
      val numQuestions = rounds.head.questions.size
      for (round <- rounds) {
        require(
          round.questions.size == numQuestions,
          s"zipRoundsWithGeneticRoundNames is true, but not all rounds have the same amount of questions (${round.name})",
        )
      }

      for (i <- 0 until numQuestions) yield {
        Round(
          name = i18n("app.round-n", String.valueOf(i + 1)),
          questions = rounds.map(_.questions(i)),
          expectedTime = None,
        )
      }
    }
  }

  private object RoundValue extends MapParsableValue[Round] {
    override val supportedKeyValuePairs = Map(
      "name" -> Required(StringValue),
      "questions" -> Required(ListParsableValue(QuestionValue)(_.textualQuestion)),
      "expectedTimeMinutes" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Round(
        name = map.required[String]("name"),
        questions = map.required[Seq[Question]]("questions"),
        expectedTime = map.optional[Int]("expectedTimeMinutes").map(t => Duration.ofMinutes(t.toLong)),
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
          case Some("orderItems")      => OrderItemsQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some(other) =>
            ParseResult.onlyError(
              s"questionType expected to be one of these: [unset, 'standard', 'double', 'orderItems'], but found $other"
            )
        }

      } else {
        ParseResult.onlyError(s"Expected a map but found $yamlValue")
      }
    }
  }

  private object StandardQuestionValue extends MapParsableValue[Question.Standard] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "tag" -> Optional(StringValue),
      "choices" -> Optional(ListParsableValue(StringValue)(s => s)),
      "answer" -> Required(StringValue),
      "answerDetail" -> Optional(StringValue),
      "answerImage" -> Optional(ImageValue),
      "masterNotes" -> Optional(StringValue),
      "image" -> Optional(ImageValue),
      "audioSrc" -> Optional(StringValue),
      "videoSrc" -> Optional(StringValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
      "pointsToGainOnFirstAnswer" -> Optional(FixedPointNumberValue),
      "pointsToGainOnWrongAnswer" -> Optional(FixedPointNumberValue),
      "maxTimeSeconds" -> Optional(IntValue),
      "onlyFirstGainsPoints" -> Optional(BooleanValue),
      "showSingleAnswerButtonToTeams" -> Optional(BooleanValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.Standard(
        question = map.required[String]("question"),
        questionDetail = map.optional("questionDetail"),
        tag = map.optional("tag"),
        choices = map.optional("choices"),
        answer = map.required[String]("answer"),
        answerDetail = map.optional("answerDetail"),
        answerImage = map.optional("answerImage"),
        masterNotes = map.optional("masterNotes"),
        image = map.optional("image"),
        audioSrc = map.optional("audioSrc"),
        videoSrc = map.optional("videoSrc"),
        pointsToGain = map.optional("pointsToGain", FixedPointNumber(1)),
        pointsToGainOnFirstAnswer = map.optional("pointsToGainOnFirstAnswer") getOrElse map
          .optional("pointsToGain", FixedPointNumber(1)),
        pointsToGainOnWrongAnswer = map.optional("pointsToGainOnWrongAnswer", FixedPointNumber(0)),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", 180)),
        onlyFirstGainsPoints = map.optional("onlyFirstGainsPoints", false),
        showSingleAnswerButtonToTeams = map.optional("showSingleAnswerButtonToTeams", false),
      )
    }
    override def additionalValidationErrors(v: Question.Standard) = {
      Seq(
        v.validationErrors(),
        v.image.map(_.src).flatMap(quizAssets.imageExistsOrValidationError).toSet,
        v.answerImage.map(_.src).flatMap(quizAssets.imageExistsOrValidationError).toSet,
        v.audioSrc.flatMap(quizAssets.audioExistsOrValidationError).toSet,
        v.videoSrc.flatMap(quizAssets.videoExistsOrValidationError).toSet,
      ).flatten
    }
  }

  private object DoubleQuestionValue extends MapParsableValue[Question.DoubleQ] {
    override val supportedKeyValuePairs = Map(
      "verbalQuestion" -> Required(StringValue),
      "verbalAnswer" -> Required(StringValue),
      "textualQuestion" -> Required(StringValue),
      "textualAnswer" -> Required(StringValue),
      "textualChoices" -> Required(ListParsableValue(StringValue)(s => s)),
      "pointsToGain" -> Optional(FixedPointNumberValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.DoubleQ(
        verbalQuestion = map.required[String]("verbalQuestion"),
        verbalAnswer = map.required[String]("verbalAnswer"),
        textualQuestion = map.required[String]("textualQuestion"),
        textualAnswer = map.required[String]("textualAnswer"),
        textualChoices = map.required[Seq[String]]("textualChoices"),
        pointsToGain = map.optional("pointsToGain", FixedPointNumber(2)),
      )
    }
    override def additionalValidationErrors(v: Question.DoubleQ) = v.validationErrors()
  }

  private object OrderItemsQuestionValue extends MapParsableValue[Question.OrderItems] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "tag" -> Optional(StringValue),
      "orderedItemsThatWillBePresentedInAlphabeticalOrder" -> Required(
        ListParsableValue(OrderItemValue)(_.item)
      ),
      "answerDetail" -> Optional(StringValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
      "maxTimeSeconds" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.OrderItems(
        question = map.required[String]("question"),
        questionDetail = map.optional("questionDetail"),
        tag = map.optional("tag"),
        orderedItemsThatWillBePresentedInAlphabeticalOrder =
          map.required[Seq[Question.OrderItems.Item]]("orderedItemsThatWillBePresentedInAlphabeticalOrder"),
        answerDetail = map.optional("answerDetail"),
        pointsToGain = map.optional("pointsToGain", FixedPointNumber(1)),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", 180)),
      )
    }
    override def additionalValidationErrors(v: Question.OrderItems) = v.validationErrors()
  }

  private val OrderItemValue: ParsableValue[Question.OrderItems.Item] = {
    WithStringSimplification(RawOrderItemValue)(
      stringToValue = s =>
        Question.OrderItems.Item(
          item = s,
          answerDetail = None,
        )
    )
  }

  private object RawOrderItemValue extends MapParsableValue[Question.OrderItems.Item] {
    override val supportedKeyValuePairs = Map(
      "item" -> Required(StringValue),
      "answerDetail" -> Optional(StringValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.OrderItems.Item(
        item = map.required[String]("item"),
        answerDetail = map.optional("answerDetail"),
      )
    }
  }

  private val ImageValue: ParsableValue[Image] = {
    WithStringSimplification(RawImageValue)(
      stringToValue = s =>
        Image(
          src = s,
          size = "large",
        )
    )
  }

  private object RawImageValue extends MapParsableValue[Image] {
    override val supportedKeyValuePairs = Map(
      "src" -> Required(StringValue),
      "size" -> Optional(StringValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Image(
        src = map.required[String]("src"),
        size = map.optional("size", "large"),
      )
    }
    override def additionalValidationErrors(v: Image) = {
      Seq(
        v.validationErrors(),
        quizAssets.imageExistsOrValidationError(v.src).toSet,
      ).flatten
    }
  }

  object FixedPointNumberValue extends ParsableValue[FixedPointNumber] {
    override def parse(yamlValue: Any) = {
      yamlValue match {
        case v: java.lang.Integer => ParseResult.success(FixedPointNumber(v.toInt))
        case v: java.lang.Long    => ParseResult.success(FixedPointNumber(v.toInt))
        case v: java.lang.Double  => ParseResult.success(FixedPointNumber(v.toDouble))
        case _                    => ParseResult.onlyError(s"Expected number but found $yamlValue")
      }
    }
  }
}
