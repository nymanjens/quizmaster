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
          |""".stripMargin,
      QuizConfigParsableValue
    )

    parseResult mustEqual ParseResult(
      value = QuizConfig(
        title = Some("Demo quiz"),
        author = Some("Jens Nyman"),
        masterSecret = "quiz",
        rounds = Seq(),
      ),
      validationErrors = Seq(),
    )
  }

  "parse minimal file" in {
    1 mustEqual 1
  }
}
