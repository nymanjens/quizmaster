package app.models.quiz.config

import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ParsableMapValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ParsableMapValue.MaybeRequiredMapValue.Optional
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ParsableMapValue.MaybeRequiredMapValue.Required
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.StringValue
import app.models.quiz.config.ValidatingYamlParser.ParseResult
import hydro.common.GuavaReplacement.Preconditions.checkNotNull

import scala.collection.immutable.Seq

object QuizConfigParsableValue extends ParsableMapValue[QuizConfig] {
  override val supportedKeyValuePairs = Map(
    "rounds" -> Required(StringValue()),
    "title" -> Optional(StringValue(defaultValue = null)),
    "author" -> Optional(StringValue(defaultValue = null)),
    "masterSecret" -> Optional(StringValue(defaultValue = "*")),
  )

  override def parseFromParsedMapValues(map: Map[String, Any]) = {
    ParseResult(
      QuizConfig(
        rounds = Seq(),
        title = Option(map("title").asInstanceOf[String]),
        author = Option(map("author").asInstanceOf[String]),
        masterSecret = checkNotNull(map("masterSecret").asInstanceOf[String]),
      )
    )
  }
}
