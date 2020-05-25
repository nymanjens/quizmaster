package app.models.quiz.config

import app.models.quiz.config.ValidatingYamlParser.ParsableValue.ParsableMapValue.MaybeRequiredMapValue
import app.models.quiz.config.ValidatingYamlParser.ParseResult.ValidationError

import scala.collection.immutable.Seq
import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import org.yaml.snakeyaml.introspector.BeanAccess
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag

object ValidatingYamlParser {

  def parse[V](fileContent: String, parsableValue: ParsableValue[V]): ParseResult[V] = {
    parsableValue.parse(new Yaml().load(fileContent))
  }

  trait ParsableValue[V] {
    def parse(yamlValue: Any): ParseResult[V]
    def defaultValue: V
  }
  object ParsableValue {
    sealed abstract class PrimitiveValue[V: ClassTag] extends ParsableValue[V] {
      override final def parse(yamlValue: Any): ParseResult[V] = {
        val clazz = implicitly[ClassTag[V]].runtimeClass
        if (clazz.isInstance(yamlValue)) {
          ParseResult(yamlValue.asInstanceOf[V])
        } else {
          ParseResult.withoutPath(
            defaultValue,
            validationErrors = Seq(s"Expected ${clazz.getSimpleName} but found $yamlValue"))
        }
      }
    }
    case class IntValue(override val defaultValue: Int = -1) extends PrimitiveValue[Int]
    case class StringValue(override val defaultValue: String = "") extends PrimitiveValue[String]

    abstract class ParsableMapValue[V] extends ParsableValue[V] {
      override final def parse(yamlValue: Any): ParseResult[V] = {
        if (yamlValue.isInstanceOf[java.util.Map[_, _]]) {
          val yamlMap = yamlValue.asInstanceOf[java.util.Map[String, _]].asScala

          val mapWithParsedValues = mutable.Map[String, Any]()
          val validationErrors = mutable.Buffer[ValidationError]()

          for ((mapKey, mapValue) <- yamlMap) {
            if (supportedKeyValuePairs.contains(mapKey)) {
              supportedKeyValuePairs(mapKey).parsableValue.parse(mapValue) match {
                case ParseResult(parsedValue, additionalValidationErrors) =>
                  validationErrors.append(additionalValidationErrors.map(_.prependPath(mapKey + ".")): _*)
                  mapWithParsedValues.put(mapKey, parsedValue)
              }
            } else {
              validationErrors.append(ValidationError(s"Unknown field: $mapKey"))
            }
          }

          supportedKeyValuePairs.collect {
            case (mapKey, MaybeRequiredMapValue.Required(_)) if !yamlMap.contains(mapKey) =>
              validationErrors.append(ValidationError(s"Required field: $mapKey"))
          }
          supportedKeyValuePairs.collect {
            case (mapKey, mapValue) if !yamlMap.contains(mapKey) =>
              mapWithParsedValues.put(mapKey, mapValue.parsableValue.defaultValue)
          }

          parseFromParsedMapValues(mapWithParsedValues.toMap) match {
            case ParseResult(value, additionalValidationErrors) =>
              validationErrors.append(additionalValidationErrors: _*)
              ParseResult(value, validationErrors.toVector)
          }
        } else {
          ParseResult.withoutPath(
            defaultValue,
            validationErrors = Seq(s"Expected a map but found $yamlValue"))
        }
      }
      override final def defaultValue: V = {
        parseFromParsedMapValues(supportedKeyValuePairs.mapValues(_.parsableValue.defaultValue)).value
      }

      val supportedKeyValuePairs: Map[String, MaybeRequiredMapValue]
      def parseFromParsedMapValues(map: Map[String, Any]): ParseResult[V]
    }
    object ParsableMapValue {
      sealed trait MaybeRequiredMapValue {
        def parsableValue: ParsableValue[_]
      }
      object MaybeRequiredMapValue {
        case class Required(override val parsableValue: ParsableValue[_]) extends MaybeRequiredMapValue
        case class Optional(override val parsableValue: ParsableValue[_]) extends MaybeRequiredMapValue
      }
    }
  }

  case class ParseResult[V](
      value: V,
      validationErrors: Seq[ValidationError] = Seq(),
  )
  object ParseResult {
    def withoutPath[V](value: V, validationErrors: Seq[String] = Seq()): ParseResult[V] = {
      ParseResult(value, validationErrors.map(e => ValidationError(e)))
    }

    case class ValidationError(error: String, path: String = "") {
      def prependPath(pathPrefix: String): ValidationError = {
        ValidationError(error, path = pathPrefix + path)
      }

      def toErrorString: String = {
        if (path.nonEmpty) s"[$path] $error" else error
      }
    }
  }
}
