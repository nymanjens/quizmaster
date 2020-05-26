package app.models.quiz.config;

import java.nio.file.Paths
import java.time.Duration

import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round

import scala.collection.immutable.Seq
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import com.google.common.io.Files
import com.google.common.io.MoreFiles
import com.google.inject.AbstractModule
import com.google.inject.Guice
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
    val parseResult = ValidatingYamlParser.parse(
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
        |        answerImage: {src: geography/france-answer.png, size: large}
        |        masterNotes: This is a very simple question
        |        answerDetail: Image released under Creative Commons by Destination2 (www.destination2.co.uk)
        |        pointsToGain: 2
        |        pointsToGainOnFirstAnswer: 4
        |        pointsToGainOnWrongAnswer: -1
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
        |  - name: Music round
        |    questions:
        |      - question: After which season is this track named?
        |        questionDetail: (Royalty Free Music from Bensound)
        |        answer: Summer
        |        answerDetail: (By Bensound)
        |        audioSrc: music_round/bensound-summer.mp3
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
      QuizConfigParsableValue
    )

    assertNoValidationErrors(parseResult)

    parseResult.maybeValue mustEqual Some(
      QuizConfig(
        title = Some("Demo quiz"),
        author = Some("Jens Nyman"),
        masterSecret = "quiz",
        rounds = Seq(
          Round(
            name = "Geography",
            expectedTime = Some(Duration.ofMinutes(2)),
            questions = Seq(
              Question.Single(
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
                pointsToGain = 2,
                pointsToGainOnFirstAnswer = 4,
                pointsToGainOnWrongAnswer = -1,
                maxTime = Duration.ofSeconds(8),
                onlyFirstGainsPoints = true,
                showSingleAnswerButtonToTeams = false,
              ),
              Question.Single(
                question = "What is the capital of Belgium?",
                questionDetail = None,
                choices = Some(Seq("Paris", "London", "Brussels", "Berlin")),
                answer = "Brussels",
                answerDetail = None,
                answerImage = None,
                masterNotes = None,
                image = None,
                audioSrc = None,
                pointsToGain = 1,
                pointsToGainOnFirstAnswer = 1,
                pointsToGainOnWrongAnswer = 0,
                maxTime = Duration.ofSeconds(60),
                onlyFirstGainsPoints = false,
                showSingleAnswerButtonToTeams = false,
              ),
              Question.Single(
                question = "Who was the country Columbia named after?",
                questionDetail = None,
                choices = None,
                answer = "Christoffer Columbus",
                answerDetail = None,
                answerImage = None,
                masterNotes = None,
                image = None,
                audioSrc = None,
                pointsToGain = 1,
                pointsToGainOnFirstAnswer = 1,
                pointsToGainOnWrongAnswer = 0,
                maxTime = Duration.ofSeconds(8),
                onlyFirstGainsPoints = false,
                showSingleAnswerButtonToTeams = true,
              )
            ),
          ),
          Round(
            name = "Music round",
            expectedTime = None,
            questions = Seq(
              Question.Single(
                question = "After which season is this track named?",
                questionDetail = Some("(Royalty Free Music from Bensound)"),
                choices = None,
                answer = "Summer",
                answerDetail = Some("(By Bensound)"),
                answerImage = None,
                masterNotes = None,
                image = None,
                audioSrc = Some("music_round/bensound-summer.mp3"),
                pointsToGain = 1,
                pointsToGainOnFirstAnswer = 1,
                pointsToGainOnWrongAnswer = 0,
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
              Question.Double(
                verbalQuestion = "How many sides does a rectangle have?",
                verbalAnswer = "4",
                textualQuestion = "How many sides does a triangle have?",
                textualAnswer = "3",
                textualChoices = Seq("3", "4", "5", "6"),
                pointsToGain = 2,
              ),
            ),
          )
        ),
      ))
  }

  "parse minimal file" in {
    val parseResult = ValidatingYamlParser.parse(
      """
              |title: Demo quiz
              |rounds: []
              |""".stripMargin,
      QuizConfigParsableValue
    )

    assertNoValidationErrors(parseResult)

    parseResult.maybeValue mustEqual Some(
      QuizConfig(
        title = Some("Demo quiz"),
        author = None,
        masterSecret = "*",
        rounds = Seq(),
      )
    )
  }

  "parse demo quiz config without errors" in {
    val knownQuizConfigs =
      Seq("../../conf/demo-quiz-config.yml.old") ++
        recursivelyFindYamlFiles("../../../hydro/quizmaster/conf/quiz")

    for (knownQuizConfig <- knownQuizConfigs) yield {
      val injector =
        Guice.createInjector(
          new AbstractModule {
            override def configure(): Unit = {
              bind(classOf[play.api.Configuration])
                .toInstance(play.api.Configuration("app.quiz.configYamlFilePath" -> knownQuizConfig))
            }
          },
          new ConfigModule(exitOnFailure = false),
        )

      injector.getInstance(classOf[QuizConfig]) mustNotEqual null
    }
  }

  private def assertNoValidationErrors(parseResult: ParseResult[_]): Unit = {
    require(
      parseResult.validationErrors.isEmpty,
      s"""Found validation errors:
         |${parseResult.validationErrors.map(e => s"  - ${e.toErrorString}\n").mkString}
         |
         |FYI: The parsed value was:
         |${parseResult.maybeValue}
         |""".stripMargin,
    )
  }

  private def recursivelyFindYamlFiles(rootPath: String): Seq[String] = {
    for {
      path <- MoreFiles.fileTraverser().breadthFirst(Paths.get(rootPath)).asScala.toVector
      if MoreFiles.getFileExtension(path) == "yml"
      if !(path.toString contains "/0_")
    } yield path.toString
  }
}
