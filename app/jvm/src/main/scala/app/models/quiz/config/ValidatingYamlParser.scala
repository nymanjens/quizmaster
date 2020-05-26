package app.models.quiz.config

import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Optional
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.MaybeRequiredMapValue.Required
import app.models.quiz.config.ValidatingYamlParser.ParsableValue.MapParsableValue.StringMap
import app.models.quiz.config.ValidatingYamlParser.ParseResult.ValidationError
import org.yaml.snakeyaml.Yaml

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag

object ValidatingYamlParser {

  def parse[V](fileContent: String, parsableValue: ParsableValue[V]): ParseResult[V] = {
    parsableValue.parse(new Yaml().load(fileContent))
  }

  trait ParsableValue[V] {
    def parse(yamlValue: Any): ParseResult[V]
  }
  object ParsableValue {
    sealed abstract class PrimitiveValue[V: ClassTag] extends ParsableValue[V] {
      override final def parse(yamlValue: Any): ParseResult[V] = {
        val clazz = implicitly[ClassTag[V]].runtimeClass
        if (clazz.isInstance(yamlValue)) {
          ParseResult.success(yamlValue.asInstanceOf[V])
        } else {
          ParseResult.onlyError(s"Expected ${clazz.getSimpleName} but found $yamlValue")
        }
      }
    }
    object IntValue extends PrimitiveValue[Int]
    object StringValue extends PrimitiveValue[String]
    object BooleanValue extends PrimitiveValue[Boolean]

    class ListParsableValue[V](itemParsableValue: ParsableValue[V], errorPathString: V => String)
        extends ParsableValue[Seq[V]] {
      override final def parse(yamlValue: Any): ParseResult[Seq[V]] = {
        if (yamlValue.isInstanceOf[java.util.List[_]]) {
          val yamlList = yamlValue.asInstanceOf[java.util.List[_]].asScala.toVector

          val validationErrors = mutable.Buffer[ValidationError]()

          val parsedValuesSeq =
            for ((item, index) <- yamlList.zipWithIndex) yield {
              itemParsableValue.parse(item) match {
                case ParseResult(parsedValue, additionalValidationErrors) =>
                  val pathPrefix = parsedValue match {
                    case Some(v) =>
                      errorPathString(v) match {
                        case s if s.length < 20 => s
                        case s                  => s.take(17).trim + "…"
                      }
                    case None => index.toString
                  }
                  validationErrors.append(additionalValidationErrors.map(_.prependPath(pathPrefix)): _*)
                  parsedValue
              }
            }
          ParseResult(Some(parsedValuesSeq.flatten), validationErrors.toVector)
        } else {
          ParseResult.onlyError(s"Expected a list but found $yamlValue")
        }
      }
    }
    object ListParsableValue {
      def apply[V](itemParsableValue: ParsableValue[V])(
          errorPathString: V => String): ListParsableValue[V] = {
        new ListParsableValue[V](itemParsableValue, errorPathString)
      }
    }
    abstract class MapParsableValue[V] extends ParsableValue[V] {
      override final def parse(yamlValue: Any): ParseResult[V] = {
        if (yamlValue.isInstanceOf[java.util.Map[_, _]]) {
          val yamlMap = yamlValue.asInstanceOf[java.util.Map[String, _]].asScala

          val mapWithParsedValues = mutable.Map[String, Any]()
          val validationErrors = mutable.Buffer[ValidationError]()

          for ((mapKey, mapValue) <- yamlMap) {
            if (supportedKeyValuePairs.contains(mapKey)) {
              supportedKeyValuePairs(mapKey).parsableValue.parse(mapValue) match {
                case ParseResult(maybeParsedValue, additionalValidationErrors) =>
                  validationErrors.append(additionalValidationErrors.map(_.prependPath(mapKey)): _*)
                  for (parsedValue <- maybeParsedValue) {
                    mapWithParsedValues.put(mapKey, parsedValue)
                  }
              }
            } else {
              validationErrors.append(ValidationError(s"Unknown field: $mapKey"))
            }
          }

          supportedKeyValuePairs.collect {
            case (mapKey, MaybeRequiredMapValue.Required(_, valueIfInvalid)) if !yamlMap.contains(mapKey) =>
              validationErrors.append(ValidationError(s"Required field: $mapKey"))
              mapWithParsedValues.put(mapKey, valueIfInvalid)
          }
          supportedKeyValuePairs.collect {
            case (mapKey, mapValue) if !yamlMap.contains(mapKey) =>
          }

          parseFromParsedMapValues(
            StringMap(mapWithParsedValues.toMap, supportedKeyValuePairs)
          ) match {
            case ParseResult(value, additionalValidationErrors) =>
              validationErrors.append(additionalValidationErrors: _*)
              ParseResult(value.asInstanceOf[Option[V]], validationErrors.toVector)
          }
        } else {
          ParseResult.onlyError(s"Expected a map but found $yamlValue")
        }
      }

      val supportedKeyValuePairs: Map[String, MaybeRequiredMapValue]
      def parseFromParsedMapValues(map: StringMap): ParseResult[V]
      def additionalValidationErrors(parsedValue: V): Seq[String] = Seq()
    }
    object MapParsableValue {
      case class StringMap(
          private val delegate: Map[String, Any],
          private val supportedKeyValuePairs: Map[String, MaybeRequiredMapValue],
      ) {
        def required[V](mapKey: String): V = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Required[_]])
          get[V](mapKey) getOrElse supportedKeyValuePairs(mapKey).asInstanceOf[Required[V]].valueIfInvalid
        }
        def optional[V](mapKey: String): Option[V] = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Optional])
          get(mapKey)
        }
        def optional[V](mapKey: String, defaultValue: V): V = {
          require(supportedKeyValuePairs(mapKey).isInstanceOf[Optional])
          get[V](mapKey) getOrElse checkNotNull(defaultValue)
        }

        private def get[V](mapKey: String): Option[V] = {
          delegate.get(mapKey).asInstanceOf[Option[V]].map(checkNotNull)
        }
      }

      sealed trait MaybeRequiredMapValue {
        def parsableValue: ParsableValue[_]
      }
      object MaybeRequiredMapValue {
        case class Required[V](override val parsableValue: ParsableValue[V], valueIfInvalid: V)
            extends MaybeRequiredMapValue
        object Required {
          def apply[V](v: ListParsableValue[V]): Required[Seq[V]] = Required(v, valueIfInvalid = Seq())
          def apply(v: IntValue.type): Required[Int] = Required(v, valueIfInvalid = 0)
          def apply(v: StringValue.type): Required[String] = Required(v, valueIfInvalid = "")

        }
        case class Optional(override val parsableValue: ParsableValue[_]) extends MaybeRequiredMapValue
      }
    }
  }

  case class ParseResult[+V](
      maybeValue: Option[V],
      validationErrors: Seq[ValidationError] = Seq(),
  )
  object ParseResult {
    def onlyError[V](validationError: String): ParseResult[V] = {
      ParseResult[V](maybeValue = None, validationErrors = Seq(ValidationError(validationError)))
    }
    def success[V](value: V): ParseResult[V] = {
      ParseResult(Some(value))
    }

    case class ValidationError(error: String, path: String = "") {
      def prependPath(pathPrefix: String): ValidationError = {
        ValidationError(error, path = if (path.nonEmpty) s"$pathPrefix > $path" else pathPrefix)
      }

      def toErrorString: String = {
        if (path.nonEmpty) s"[$path] $error" else error
      }
    }
  }
}
