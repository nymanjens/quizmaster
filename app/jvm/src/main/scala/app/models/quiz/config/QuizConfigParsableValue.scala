package app.models.quiz.config

import java.time.Duration

import app.common.FixedPointNumber
import app.common.QuizAssets

import scala.collection.JavaConverters._
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Question.MultipleQuestions.SubQuestion
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.config.QuizConfig.UsageStatistics
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
import hydro.common.PlayI18n
import play.api.i18n.MessagesApi

import scala.collection.immutable.Seq

class QuizConfigParsableValue @Inject() (implicit
    quizAssets: QuizAssets,
    messagesApi: MessagesApi,
) extends MapParsableValue[QuizConfig] {

  private val magicDefaultMaxTimeSeconds: Int = -129379821
  private val magicDefaultPointsToGain: FixedPointNumber = FixedPointNumber(-918237)

  override val supportedKeyValuePairs = Map(
    "title" -> Optional(StringValue),
    "author" -> Optional(StringValue),
    "instructionsOnFirstSlide" -> Optional(StringValue),
    "masterSecret" -> Optional(StringValue),
    "zipRoundsWithGenericRoundNames" -> Optional(BooleanValue),
    "language" -> Optional(StringValue),
    "usageStatistics" -> Optional(UsageStatisticsValue),
    "defaults" -> Optional(QuizConfigDefaultsValue),
    "rounds" -> Required(ListParsableValue(RoundValue)(_.name)),
  )

  override def parseFromParsedMapValues(map: StringMap) = {
    val quizConfigDefaults =
      map.optional("defaults", QuizConfigDefaultsValue.parse(new java.util.HashMap()).maybeValue.get)
    val languageCode = map.optional("language", "en")

    replaceMagicDefaultsByConfiguredDefaults(
      quizConfigDefaults,
      QuizConfig(
        title = map.optional("title"),
        author = map.optional("author"),
        instructionsOnFirstSlide = map.optional("instructionsOnFirstSlide"),
        masterSecret = map.optional("masterSecret", "*"),
        rounds = {
          val rounds = map.required[Seq[Round]]("rounds")
          if (map.optional("zipRoundsWithGenericRoundNames", false))
            zipRoundsWithGenericRoundNames(rounds, languageCode)
          else rounds
        },
        languageCode = languageCode,
        usageStatistics = map.optional("usageStatistics", UsageStatistics.default),
      ),
    )
  }

  private def replaceMagicDefaultsByConfiguredDefaults(
      defaults: QuizConfigDefaults,
      config: QuizConfig,
  ): QuizConfig = {
    def replacePoints(
        points: FixedPointNumber,
        replacementForMagic: FixedPointNumber = defaults.pointsToGain,
    ): FixedPointNumber = {
      if (points == magicDefaultPointsToGain) replacementForMagic else points
    }
    def replaceMaxTime(
        maxTime: Duration
    ): Duration = {
      if (maxTime == Duration.ofSeconds(magicDefaultMaxTimeSeconds))
        Duration.ofSeconds(defaults.maxTimeSeconds)
      else maxTime
    }

    config.copy(rounds =
      config.rounds.map(round =>
        round.copy(questions = round.questions.map {
          case q: Question.Standard =>
            q.copy(
              pointsToGain = replacePoints(q.pointsToGain),
              pointsToGainOnFirstAnswer = replacePoints(q.pointsToGainOnFirstAnswer),
              maxTime = replaceMaxTime(q.maxTime),
            )
          case q: Question.MultipleAnswers =>
            q.copy(
              pointsToGain =
                replacePoints(q.pointsToGain, defaults.multipleAnswersPointsToGainPerAnswer * q.answers.size),
              maxTime = replaceMaxTime(q.maxTime),
            )
          case q: Question.MultipleQuestions =>
            q.copy(
              subQuestions = q.subQuestions.map(subQ =>
                subQ.copy(pointsToGain =
                  replacePoints(subQ.pointsToGain, defaults.multipleQuestionsPointsToGainPerQuestion)
                )
              ),
              maxTime = replaceMaxTime(q.maxTime),
            )
          case q: Question.DoubleQ =>
            q.copy(pointsToGain = replacePoints(q.pointsToGain))
          case q: Question.OrderItems =>
            q.copy(
              pointsToGain = replacePoints(q.pointsToGain, defaults.orderItemsPointsToGainInTotal),
              maxTime = replaceMaxTime(q.maxTime),
            )
        })
      )
    )
  }

  private def zipRoundsWithGenericRoundNames(rounds: Seq[Round], languageCode: String): Seq[Round] = {
    val i18n = PlayI18n.fromLanguageCode(languageCode)

    if (rounds.isEmpty) {
      rounds
    } else {
      val numQuestions = rounds.head.questions.size
      for (round <- rounds) {
        require(
          round.questions.size == numQuestions,
          s"zipRoundsWithGenericRoundNames is true, but not all rounds have the same amount of questions (${round.name})",
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

  private object UsageStatisticsValue extends MapParsableValue[UsageStatistics] {
    override val supportedKeyValuePairs = Map(
      "sendAnonymousUsageDataAtEndOfQuiz" -> Optional(BooleanValue),
      "includeAuthor" -> Optional(BooleanValue),
      "includeQuizTitle" -> Optional(BooleanValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      UsageStatistics(
        sendAnonymousUsageDataAtEndOfQuiz = map.optional("sendAnonymousUsageDataAtEndOfQuiz", false),
        includeAuthor = map.optional("includeAuthor", false),
        includeQuizTitle = map.optional("includeQuizTitle", false),
      )
    }
  }

  private case class QuizConfigDefaults(
      maxTimeSeconds: Int,
      pointsToGain: FixedPointNumber,
      multipleAnswersPointsToGainPerAnswer: FixedPointNumber,
      multipleQuestionsPointsToGainPerQuestion: FixedPointNumber,
      orderItemsPointsToGainInTotal: FixedPointNumber,
  )

  private object QuizConfigDefaultsValue extends MapParsableValue[QuizConfigDefaults] {
    override val supportedKeyValuePairs = Map(
      "maxTimeSeconds" -> Optional(IntValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
      "multipleAnswers_pointsToGainPerAnswer" -> Optional(FixedPointNumberValue),
      "multipleQuestions_pointsToGainPerQuestion" -> Optional(FixedPointNumberValue),
      "orderItems_pointsToGainInTotal" -> Optional(FixedPointNumberValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      val defaultPointsToGain = map.optional("pointsToGain", FixedPointNumber(1))
      QuizConfigDefaults(
        maxTimeSeconds = map.optional("maxTimeSeconds", 120),
        pointsToGain = defaultPointsToGain,
        multipleAnswersPointsToGainPerAnswer =
          map.optional("multipleAnswers_pointsToGainPerAnswer", defaultPointsToGain),
        multipleQuestionsPointsToGainPerQuestion =
          map.optional("multipleQuestions_pointsToGainPerQuestion", defaultPointsToGain),
        orderItemsPointsToGainInTotal = map.optional("orderItems_pointsToGainInTotal", defaultPointsToGain),
      )
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
          case None | Some("standard")   => StandardQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some("double")            => DoubleQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some("orderItems")        => OrderItemsQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some("multipleAnswers")   => MultipleAnswersQuestionValue.parse(yamlMapWithoutQuestionType)
          case Some("multipleQuestions") => MultipleQuestionsQuestionValue.parse(yamlMapWithoutQuestionType)
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
      "masterNotes" -> Optional(StringValue),
      "image" -> Optional(ImageValue),
      "answerImage" -> Optional(ImageValue),
      "audio" -> Optional(StringValue),
      "answerAudio" -> Optional(StringValue),
      "video" -> Optional(StringValue),
      "answerVideo" -> Optional(StringValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
      "pointsToGainOnFirstAnswer" -> Optional(FixedPointNumberValue),
      "pointsToGainOnWrongAnswer" -> Optional(FixedPointNumberValue),
      "maxTimeSeconds" -> Optional(IntValue),
      "onlyFirstGainsPoints" -> Optional(BooleanValue),
      "showSingleAnswerButtonToTeams" -> Optional(BooleanValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      val pointsToGain = map.optional("pointsToGain", magicDefaultPointsToGain)
      Question.Standard(
        question = map.required[String]("question"),
        questionDetail = map.optional("questionDetail"),
        masterNotes = map.optional("masterNotes"),
        tag = map.optional("tag"),
        choices = map.optional("choices"),
        answer = map.required[String]("answer"),
        answerDetail = map.optional("answerDetail"),
        image = map.optional("image"),
        answerImage = map.optional("answerImage"),
        audioSrc = map.optional("audio"),
        answerAudioSrc = map.optional("answerAudio"),
        videoSrc = map.optional("video"),
        answerVideoSrc = map.optional("answerVideo"),
        pointsToGain = pointsToGain,
        pointsToGainOnFirstAnswer = map.optional("pointsToGainOnFirstAnswer") getOrElse pointsToGain,
        pointsToGainOnWrongAnswer = map.optional("pointsToGainOnWrongAnswer", FixedPointNumber(0)),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", magicDefaultMaxTimeSeconds)),
        onlyFirstGainsPoints = map.optional("onlyFirstGainsPoints", false),
        showSingleAnswerButtonToTeams = map.optional("showSingleAnswerButtonToTeams", false),
      )
    }
    override def additionalValidationErrors(v: Question.Standard) = {
      Seq(
        v.validationErrors(),
        v.image.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.answerImage.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.audioSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.videoSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
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
        pointsToGain = map.optional("pointsToGain", magicDefaultPointsToGain),
      )
    }
    override def additionalValidationErrors(v: Question.DoubleQ) = v.validationErrors()
  }

  private object MultipleAnswersQuestionValue extends MapParsableValue[Question.MultipleAnswers] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "masterNotes" -> Optional(StringValue),
      "tag" -> Optional(StringValue),
      "answers" -> Required(ListParsableValue(MultipleAnswersAnswerValue)(_.answer)),
      "answersHaveToBeInSameOrder" -> Required(BooleanValue),
      "answerDetail" -> Optional(StringValue),
      "image" -> Optional(ImageValue),
      "answerImage" -> Optional(ImageValue),
      "audio" -> Optional(StringValue),
      "answerAudio" -> Optional(StringValue),
      "video" -> Optional(StringValue),
      "answerVideo" -> Optional(StringValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
      "maxTimeSeconds" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.MultipleAnswers(
        question = map.required[String]("question"),
        questionDetail = map.optional("questionDetail"),
        masterNotes = map.optional("masterNotes"),
        tag = map.optional("tag"),
        answers = map.required[Seq[Question.MultipleAnswers.Answer]]("answers"),
        answersHaveToBeInSameOrder = map.required[Boolean]("answersHaveToBeInSameOrder"),
        answerDetail = map.optional("answerDetail"),
        image = map.optional("image"),
        answerImage = map.optional("answerImage"),
        audioSrc = map.optional("audio"),
        answerAudioSrc = map.optional("answerAudio"),
        videoSrc = map.optional("video"),
        answerVideoSrc = map.optional("answerVideo"),
        pointsToGain = map.optional("pointsToGain", magicDefaultPointsToGain),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", magicDefaultMaxTimeSeconds)),
      )
    }
    override def additionalValidationErrors(v: Question.MultipleAnswers) = {
      Seq(
        v.validationErrors(),
        v.image.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.answerImage.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.audioSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.videoSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
      ).flatten
    }
  }
  private val MultipleAnswersAnswerValue: ParsableValue[Question.MultipleAnswers.Answer] = {
    WithStringSimplification(RawMultipleAnswersAnswerValue)(
      stringToValue = s =>
        Question.MultipleAnswers.Answer(
          answer = s,
          answerDetail = None,
        )
    )
  }
  private object RawMultipleAnswersAnswerValue extends MapParsableValue[Question.MultipleAnswers.Answer] {
    override val supportedKeyValuePairs = Map(
      "answer" -> Required(StringValue),
      "answerDetail" -> Optional(StringValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.MultipleAnswers.Answer(
        answer = map.required[String]("answer"),
        answerDetail = map.optional("answerDetail"),
      )
    }
  }

  private object MultipleQuestionsQuestionValue extends MapParsableValue[Question.MultipleQuestions] {
    override val supportedKeyValuePairs = Map(
      "questionTitle" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "masterNotes" -> Optional(StringValue),
      "tag" -> Optional(StringValue),
      "questions" -> Required(ListParsableValue(SubQuestionValue)(_.question)),
      "answerDetail" -> Optional(StringValue),
      "image" -> Optional(ImageValue),
      "answerImage" -> Optional(ImageValue),
      "audio" -> Optional(StringValue),
      "answerAudio" -> Optional(StringValue),
      "video" -> Optional(StringValue),
      "answerVideo" -> Optional(StringValue),
      "maxTimeSeconds" -> Optional(IntValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.MultipleQuestions(
        questionTitle = map.required[String]("questionTitle"),
        questionDetail = map.optional("questionDetail"),
        masterNotes = map.optional("masterNotes"),
        tag = map.optional("tag"),
        subQuestions = map.required[Seq[SubQuestion]]("questions"),
        answerDetail = map.optional("answerDetail"),
        image = map.optional("image"),
        answerImage = map.optional("answerImage"),
        audioSrc = map.optional("audio"),
        answerAudioSrc = map.optional("answerAudio"),
        videoSrc = map.optional("video"),
        answerVideoSrc = map.optional("answerVideo"),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", magicDefaultMaxTimeSeconds)),
      )
    }

    override def additionalValidationErrors(v: Question.MultipleQuestions) = {
      Seq(
        v.validationErrors(),
        v.image.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.answerImage.map(_.src).flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.audioSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
        v.videoSrc.flatMap(quizAssets.assetExistsOrValidationError).toSet,
      ).flatten
    }
  }

  private object SubQuestionValue extends MapParsableValue[Question.MultipleQuestions.SubQuestion] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "answer" -> Required(StringValue),
      "answerDetail" -> Optional(StringValue),
      "pointsToGain" -> Optional(FixedPointNumberValue),
    )
    override def parseFromParsedMapValues(map: StringMap) = {
      Question.MultipleQuestions.SubQuestion(
        question = map.required[String]("question"),
        answer = map.required[String]("answer"),
        answerDetail = map.optional("answerDetail"),
        pointsToGain = map.optional("pointsToGain", magicDefaultPointsToGain),
      )
    }
  }

  private object OrderItemsQuestionValue extends MapParsableValue[Question.OrderItems] {
    override val supportedKeyValuePairs = Map(
      "question" -> Required(StringValue),
      "questionDetail" -> Optional(StringValue),
      "masterNotes" -> Optional(StringValue),
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
        masterNotes = map.optional("masterNotes"),
        tag = map.optional("tag"),
        orderedItemsThatWillBePresentedInAlphabeticalOrder =
          map.required[Seq[Question.OrderItems.Item]]("orderedItemsThatWillBePresentedInAlphabeticalOrder"),
        answerDetail = map.optional("answerDetail"),
        pointsToGain = map.optional("pointsToGain", magicDefaultPointsToGain),
        maxTime = Duration.ofSeconds(map.optional[Int]("maxTimeSeconds", magicDefaultMaxTimeSeconds)),
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
        quizAssets.assetExistsOrValidationError(v.src).toSet,
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
