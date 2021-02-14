package app.models.quiz.config;

import java.nio.file.Paths
import java.time.Duration

import app.common.FixedPointNumber
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Question.MultipleQuestions.SubQuestion
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.config.QuizConfig.UsageStatistics
import com.google.common.io.MoreFiles
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Module
import hydro.common.I18n
import org.junit.runner._
import org.specs2.mutable.Specification
import org.specs2.runner._
import play.api.i18n.DefaultLangs
import play.api.i18n.DefaultMessagesApi
import play.api.i18n.Lang
import play.api.i18n.MessagesApi

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class QuizConfigParsableValueTest extends Specification {

  "parse maximal file" in {
    val quizConfig = ValidatingYamlParser.parse(
      """
        |title: Demo quiz
        |author: Jens Nyman
        |instructionsOnFirstSlide: Don't look up the answers online
        |masterSecret: quiz # Remove this line to allow anyone to access the master controls
        |language: nl
        |usageStatistics:
        |    sendAnonymousUsageDataAtEndOfQuiz: true
        |    includeAuthor: true
        |    includeQuizTitle: true
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
        |
        |  - name: Music round
        |    questions:
        |      - question: After which season is this track named?
        |        questionDetail: (Royalty Free Music from Bensound)
        |        answer: Summer
        |        answerDetail: (By Bensound)
        |        audio: music_round/bensound-summer.mp3
        |        answerAudio: music_round/bensound-summer.mp3
        |        video: geography/about_bananas.mp4
        |        answerVideo: geography/about_bananas.mp4
        |        maxTimeSeconds: 15
        |
        |      - questionType: multipleAnswers
        |        question: List the band members of Metallica
        |        questionDetail: (in 2021)
        |        tag: metallica
        |        answers: [James Hetfield, Lars Ulrich, Kirk Hammett, Robert Trujillo]
        |        answersHaveToBeInSameOrder: false
        |        answerDetail: Robert Trujillo joined Metallica in 2003.
        |        image: https://upload.wikimedia.org/wikipedia/commons/9/9e/Metallica_-_2003.jpg
        |        pointsToGain: 3
        |        maxTimeSeconds: 130
        |
        |      - questionType: multipleQuestions
        |        questionTitle: Can you recognize the song from the lyrics?
        |        tag: Music
        |        questionDetail: |
        |          With the lights out, it's less dangerous
        |          Here we are now, entertain us
        |        questions:
        |          - {question: "What is the song title?", answer: "Smells Like Teen Spirit"}
        |          - {question: "Which artist?", answer: Nirvana}
        |          - {question: "What year was the song released?", answer: 1991, pointsToGain: 2}
        |        answerDetail: From the album Nevermind
        |        maxTimeSeconds: 66
        |
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
      createQuizConfigParsableValue("../../conf/quiz/demo-quiz-config.yml"),
    )

    quizConfig mustEqual QuizConfig(
      title = Some("Demo quiz"),
      author = Some("Jens Nyman"),
      instructionsOnFirstSlide = Some("Don't look up the answers online"),
      masterSecret = "quiz",
      languageCode = "nl",
      usageStatistics = UsageStatistics(
        sendAnonymousUsageDataAtEndOfQuiz = true,
        includeAuthor = true,
        includeQuizTitle = true,
      ),
      rounds = Seq(
        Round(
          name = "Geography",
          expectedTime = Some(Duration.ofMinutes(2)),
          questions = Seq(
            Question.Standard(
              question = "What is the capital of France?",
              questionDetail = None,
              tag = None,
              choices = Some(Seq("Paris", "London", "Brussels", "Berlin")),
              answer = "Paris",
              answerDetail =
                Some("Image released under Creative Commons by Destination2 (www.destination2.co.uk)"),
              answerImage = Some(Image("geography/france-answer.png", "large")),
              masterNotes = Some("This is a very simple question"),
              image = Some(Image("geography/france.png", "small")),
              audioSrc = None,
              answerAudioSrc = None,
              videoSrc = None,
              answerVideoSrc = None,
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
              tag = None,
              choices = Some(Seq("Paris", "London", "Brussels", "Berlin")),
              answer = "Brussels",
              answerDetail = None,
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = None,
              answerAudioSrc = None,
              videoSrc = None,
              answerVideoSrc = None,
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
              tag = None,
              choices = None,
              answer = "Christoffer Columbus",
              answerDetail = None,
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = None,
              answerAudioSrc = None,
              videoSrc = None,
              answerVideoSrc = None,
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
              tag = None,
              masterNotes = None,
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
              tag = None,
              masterNotes = None,
              orderedItemsThatWillBePresentedInAlphabeticalOrder = Seq(
                Question.OrderItems.Item(item = "Riga", answerDetail = Some("~600k")),
                Question.OrderItems.Item(item = "Stockholm", answerDetail = Some("~1M")),
                Question.OrderItems.Item(item = "Berlin", answerDetail = Some("~4M")),
                Question.OrderItems.Item(item = "London", answerDetail = Some("~9M")),
              ),
              answerDetail = None,
              pointsToGain = FixedPointNumber(1),
              maxTime = Duration.ofSeconds(120),
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
              tag = None,
              choices = None,
              answer = "Summer",
              answerDetail = Some("(By Bensound)"),
              answerImage = None,
              masterNotes = None,
              image = None,
              audioSrc = Some("music_round/bensound-summer.mp3"),
              answerAudioSrc = Some("music_round/bensound-summer.mp3"),
              videoSrc = Some("geography/about_bananas.mp4"),
              answerVideoSrc = Some("geography/about_bananas.mp4"),
              pointsToGain = FixedPointNumber(1),
              pointsToGainOnFirstAnswer = FixedPointNumber(1),
              pointsToGainOnWrongAnswer = FixedPointNumber(0),
              maxTime = Duration.ofSeconds(15),
              onlyFirstGainsPoints = false,
              showSingleAnswerButtonToTeams = false,
            ),
            Question.MultipleAnswers(
              question = "List the band members of Metallica",
              questionDetail = Some("(in 2021)"),
              tag = Some("metallica"),
              answers = Seq("James Hetfield", "Lars Ulrich", "Kirk Hammett", "Robert Trujillo"),
              answersHaveToBeInSameOrder = false,
              answerDetail = Some("Robert Trujillo joined Metallica in 2003."),
              answerImage = None,
              masterNotes = None,
              image = Some(
                Image("https://upload.wikimedia.org/wikipedia/commons/9/9e/Metallica_-_2003.jpg", "large")
              ),
              audioSrc = None,
              answerAudioSrc = None,
              videoSrc = None,
              answerVideoSrc = None,
              pointsToGain = FixedPointNumber(3),
              maxTime = Duration.ofSeconds(130),
            ),
            Question.MultipleQuestions(
              questionTitle = "Can you recognize the song from the lyrics?",
              questionDetail =
                Some("With the lights out, it's less dangerous\nHere we are now, entertain us\n"),
              tag = Some("Music"),
              subQuestions = Seq(
                SubQuestion(
                  question = "What is the song title?",
                  answer = "Smells Like Teen Spirit",
                  pointsToGain = FixedPointNumber(1),
                ),
                SubQuestion(
                  question = "Which artist?",
                  answer = "Nirvana",
                  pointsToGain = FixedPointNumber(1),
                ),
                SubQuestion(
                  question = "What year was the song released?",
                  answer = "1991",
                  pointsToGain = FixedPointNumber(2),
                ),
              ),
              answerDetail = Some("From the album Nevermind"),
              image = None,
              answerImage = None,
              masterNotes = None,
              audioSrc = None,
              answerAudioSrc = None,
              videoSrc = None,
              answerVideoSrc = None,
              maxTime = Duration.ofSeconds(66),
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
              pointsToGain = FixedPointNumber(1),
            )
          ),
        ),
      ),
    )
  }

  "parse minimal file" in {
    val quizConfig = ValidatingYamlParser.parse(
      """
         |title: Demo quiz
         |rounds: []
         |""".stripMargin,
      createQuizConfigParsableValue("../../conf/quiz/demo-quiz-config.yml"),
    )

    quizConfig mustEqual QuizConfig(
      title = Some("Demo quiz"),
      author = None,
      instructionsOnFirstSlide = None,
      masterSecret = "*",
      languageCode = "en",
      usageStatistics = UsageStatistics.default,
      rounds = Seq(),
    )
  }

  // Test all known config files
  {
    val knownQuizConfigs =
      Seq("../../conf/quiz/demo-quiz-config.yml") ++
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
        bind(classOf[I18n]).toInstance(new I18n {
          override def apply(key: String, args: Any*): String = key
        })
        bind(classOf[MessagesApi]).toInstance(
          new DefaultMessagesApi(
            messages = Map("default" -> Map(), "en" -> Map(), "nl" -> Map()),
            langs = new DefaultLangs(Seq(Lang("en"), Lang("nl"))),
            langCookieSecure = false,
          )
        )
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
