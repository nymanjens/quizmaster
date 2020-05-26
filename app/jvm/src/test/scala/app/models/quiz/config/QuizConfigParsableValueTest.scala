package app.models.quiz.config;

import scala.collection.immutable.Seq
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import org.junit.runner._
import org.specs2.runner._
import org.specs2.mutable.Specification

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
        |    questions:
        |      - question: What is the capital of France?
        |        answer: Paris
        |        choices: [Paris, London, Brussels, Berlin]
        |        image: {src: geography/france.png, size: small}
        |        answerImage: {src: geography/france-answer.png, size: large}
        |        answerDetail: Image released under Creative Commons by Destination2 (www.destination2.co.uk)
        |        pointsToGain: 2
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

    requireNoValidationErrors(parseResult)

    parseResult.maybeValue mustEqual Some(
      QuizConfig(
        title = Some("Demo quiz"),
        author = Some("Jens Nyman"),
        masterSecret = "quiz",
        rounds = Seq(),
      )
    )
  }

  "parse minimal file" in {
    1 mustEqual 1
  }

  private def requireNoValidationErrors(parseResult: ParseResult[_]): Unit = {
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
}
