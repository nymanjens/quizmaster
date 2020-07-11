package app.models.quiz.config;

import java.nio.file.Paths
import java.time.Duration

import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round

import scala.collection.immutable.Seq
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import com.google.common.io.Files
import com.google.common.io.MoreFiles
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Module
import com.typesafe.config.ConfigFactory
import org.junit.runner._
import org.specs2.runner._
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class QuizConfigParsableValueTest extends Specification {

  "parse maximal file" in {
    val quizConfig = ValidatingYamlParser.parse(
      """
        |title: Demo quiz
        |author: Jens Nyman
        |masterSecret: quiz # Remove this line to allow anyone to access the master controls
        |
        |rounds:
        |  - name: Geography
        |    expectedTimeMinutes: 2
        |    questions:
        |      - question: What is the capital of France?
        |        answer: Paris
        |        choices: [Paris, London, Brussels, Berlin]
        |        image: {src: geography/france.png, size: small}
        |        answerImage: geography/france-answer.png
        |        masterNotes: This is a very simple question
        |        answerDetail: Image released under Creative Commons by Destination2 (www.destination2.co.uk)
        |        pointsToGain: 2.1
        |        pointsToGainOnFirstAnswer: 4.2
        |        pointsToGainOnWrongAnswer: -1.3
        |        maxTimeSeconds: 8
        |        onlyFirstGainsPoints: true
        |
        |      - question: What is the capital of Belgium?
        |        answer: Brussels
        |        choices: [Paris, London, Brussels, Berlin]
        |        maxTimeSeconds: 60
        |
        |      - question: Who was the country Columbia named after?
        |        answer: Christoffer Columbus
        |        maxTimeSeconds: 8
        |        showSingleAnswerButtonToTeams: true
        |
        |      - questionType: orderItems
        |        question: Order these cities from small to large
        |        questionDetail: Population according to Google on July 2020
        |        orderedItemsThatWillBePresentedInAlphabeticalOrder: [Riga, Stockholm, Berlin, London]
        |        answerDetail: "Riga: ~600k, Stockholm: ~1M, Berlin: ~4M, London: ~9M"
        |        pointsToGain: 2
        |        maxTimeSeconds: 180
        |
        |      - questionType: orderItems
        |        question: Order these cities from small to large
        |        orderedItemsThatWillBePresentedInAlphabeticalOrder:
        |         - {item: Riga, answerDetail: ~600k}
        |         - {item: Stockholm, answerDetail: ~1M}
        |         - {item: Berlin, answerDetail: ~4M}
        |         - {item: London, answerDetail: ~9M}
        |        maxTimeSeconds: 180
        |
        |  - name: Music round
        |    questions:
        |      - question: After which season is this track named?
        |        questionDetail: (Royalty Free Music from Bensound)
        |        answer: Summer
        |        answerDetail: (By Bensound)
        |        audioSrc: music_round/bensound-summer.mp3
        |        videoSrc: geography/about_bananas.mp4
        |        maxTimeSeconds: 15
        |
        |  - name: Double questions round
        |    questions:
        |      - questionType: double
        |        verbalQuestion: How many sides does a rectangle have?
        |        verbalAnswer: 4
        |        textualQuestion: How many sides does a triangle have?
        |        textualAnswer: 3
        |        textualChoices: [3, 4, 5, 6]
        |
        |""".stripMargin,
      createQuizConfigParsableValue("../../conf/demo-quiz-config.yml")
    )

    quizConfig mustEqual QuizConfig(
      title = Some("Demo quiz"),
      author = Some("Jens Nyman"),
      masterSecret = "quiz",
      rounds = Seq(
        Round(
          name = "Geography",
          expectedTime = Some(Duration.ofMinutes(2)),
          questions = Seq(
            Question.Standard(
              question = "What is the capital of France?",
              questionDetail = None,
              choices = Some(Seq("Paris", "London", "Brussels", "Berlin")),
              answer = "Paris",
              answerDetail =
                Some("Image released under Creative Commons by Destination2 (www.destination2.co.uk)"),
              answerImage = Some(Image("geography/france-answer.png", "large")),
              masterNotes = Some("This is a very simple question"),
              image = Some(Image("geography/france.png", "small")),
              audioSrc = None,
              videoSrc = None,
              pointsToGain = FixedPointNumber(2.1),
              pointsToGainOnFirstAnswer = FixedPointNumber(4.2),
              pointsToGainOnWrongAnswer = FixedPointNumber(-1.3),
              maxTime = Duration.ofSeconds(8),
              onlyFirstGainsPoints = true,
              showSingleAnswerButtonToTeams = false,
            ),
            Question.Standard(
              question = "What is the capital of Belgium?",
              questionDetail = None,
              choices = Some(Seq("Paris", "London", "Brussels", "Berlin")),
              answer = "Brussels",
              answerDetail = None,
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = None,
              videoSrc = None,
              pointsToGain = FixedPointNumber(1),
              pointsToGainOnFirstAnswer = FixedPointNumber(1),
              pointsToGainOnWrongAnswer = FixedPointNumber(0),
              maxTime = Duration.ofSeconds(60),
              onlyFirstGainsPoints = false,
              showSingleAnswerButtonToTeams = false,
            ),
            Question.Standard(
              question = "Who was the country Columbia named after?",
              questionDetail = None,
              choices = None,
              answer = "Christoffer Columbus",
              answerDetail = None,
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = None,
              videoSrc = None,
              pointsToGain = FixedPointNumber(1),
              pointsToGainOnFirstAnswer = FixedPointNumber(1),
              pointsToGainOnWrongAnswer = FixedPointNumber(0),
              maxTime = Duration.ofSeconds(8),
              onlyFirstGainsPoints = false,
              showSingleAnswerButtonToTeams = true,
            ),
            Question.OrderItems(
              question = "Order these cities from small to large",
              questionDetail = Some("Population according to Google on July 2020"),
              orderedItemsThatWillBePresentedInAlphabeticalOrder = Seq(
                Question.OrderItems.Item(item = "Riga", answerDetail = None),
                Question.OrderItems.Item(item = "Stockholm", answerDetail = None),
                Question.OrderItems.Item(item = "Berlin", answerDetail = None),
                Question.OrderItems.Item(item = "London", answerDetail = None),
              ),
              answerDetail = Some("Riga: ~600k, Stockholm: ~1M, Berlin: ~4M, London: ~9M"),
              pointsToGain = FixedPointNumber(2),
              maxTime = Duration.ofSeconds(180),
            ),
            Question.OrderItems(
              question = "Order these cities from small to large",
              questionDetail = None,
              orderedItemsThatWillBePresentedInAlphabeticalOrder = Seq(
                Question.OrderItems.Item(item = "Riga", answerDetail = Some("~600k")),
                Question.OrderItems.Item(item = "Stockholm", answerDetail = Some("~1M")),
                Question.OrderItems.Item(item = "Berlin", answerDetail = Some("~4M")),
                Question.OrderItems.Item(item = "London", answerDetail = Some("~9M")),
              ),
              answerDetail = None,
              pointsToGain = FixedPointNumber(1),
              maxTime = Duration.ofSeconds(180),
            ),
          ),
        ),
        Round(
          name = "Music round",
          expectedTime = None,
          questions = Seq(
            Question.Standard(
              question = "After which season is this track named?",
              questionDetail = Some("(Royalty Free Music from Bensound)"),
              choices = None,
              answer = "Summer",
              answerDetail = Some("(By Bensound)"),
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = Some("music_round/bensound-summer.mp3"),
              videoSrc = Some("geography/about_bananas.mp4"),
              pointsToGain = FixedPointNumber(1),
              pointsToGainOnFirstAnswer = FixedPointNumber(1),
              pointsToGainOnWrongAnswer = FixedPointNumber(0),
              maxTime = Duration.ofSeconds(15),
              onlyFirstGainsPoints = false,
              showSingleAnswerButtonToTeams = false,
            ),
          ),
        ),
        Round(
          name = "Double questions round",
          expectedTime = None,
          questions = Seq(
            Question.DoubleQ(
              verbalQuestion = "How many sides does a rectangle have?",
              verbalAnswer = "4",
              textualQuestion = "How many sides does a triangle have?",
              textualAnswer = "3",
              textualChoices = Seq("3", "4", "5", "6"),
              pointsToGain = FixedPointNumber(2),
            ),
          ),
        )
      ),
    )
  }

  "parse minimal file" in {
    val quizConfig = ValidatingYamlParser.parse(
      """
         |title: Demo quiz
         |rounds: []
         |""".stripMargin,
      createQuizConfigParsableValue("../../conf/demo-quiz-config.yml")
    )

    quizConfig mustEqual QuizConfig(
      title = Some("Demo quiz"),
      author = None,
      masterSecret = "*",
      rounds = Seq(),
    )
  }

  // Test all known config files
  {
    val knownQuizConfigs =
      Seq("../../conf/demo-quiz-config.yml") ++
        recursivelyFindYamlFiles("../../../hydro/quizmaster")

    for (knownQuizConfig <- knownQuizConfigs) yield {
      s"Testing known config file: $knownQuizConfig" in {
        val injector =
          Guice.createInjector(
            fakeConfigModule(knownQuizConfig),
            new ConfigModule(exitOnFailure = false),
          )

        injector.getInstance(classOf[QuizConfig]) mustNotEqual null
      }
    }
  }

  private def createQuizConfigParsableValue(configYamlFilePath: String): QuizConfigParsableValue = {
    val injector = Guice.createInjector(fakeConfigModule(configYamlFilePath))
    injector.getInstance(classOf[QuizConfigParsableValue])
  }

  private def fakeConfigModule(configYamlFilePath: String): Module = {
    new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[play.api.Configuration])
          .toInstance(play.api.Configuration("app.quiz.configYamlFilePath" -> configYamlFilePath))
      }
    }
  }

  private def recursivelyFindYamlFiles(rootPath: String): Seq[String] = {
    for {
      path <- MoreFiles.fileTraverser().depthFirstPreOrder(Paths.get(rootPath)).asScala.toVector
      if MoreFiles.getFileExtension(path) == "yml"
      if !(path.toString contains "/0_")
      if !(path.toString contains "/export")
    } yield path.toString
  }
}
