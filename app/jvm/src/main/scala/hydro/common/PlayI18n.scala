package hydro.common

import app.models.quiz.config.QuizConfig
import com.google.inject.Inject
import hydro.common
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import play.api.i18n.Lang
import play.api.i18n.Langs
import play.api.i18n.MessagesApi

trait PlayI18n extends I18n {

  /** Returns a map that maps key to the message with placeholders. */
  def allI18nMessages: Map[String, String]
}

object PlayI18n {

  def fromLanguageCode(code: String)(implicit messagesApi: MessagesApi): PlayI18n = {
    new common.PlayI18n.BaseImpl(code) {}
  }

  abstract class BaseImpl(languageCode: String)(implicit messagesApi: MessagesApi) extends PlayI18n {

    require(
      messagesApi.messages contains languageCode,
      s"The configured language code $languageCode is not supported. Supported language codes: ${messagesApi.messages.keys}",
    )

    private def defaultLang: Lang = Lang(languageCode)

    // ****************** Implementation of PlayI18n trait ****************** //
    override def apply(key: String, args: Any*): String = {
      messagesApi(key, args: _*)(defaultLang)
    }

    override val allI18nMessages: Map[String, String] = {
      // defaultLang is extended by "default" in case it didn't overwrite a message key.
      messagesApi.messages("default") ++ messagesApi.messages(defaultLang.code)
    }
  }

  final class GuiceImpl @Inject() (implicit val messagesApi: MessagesApi, quizConfig: QuizConfig)
      extends BaseImpl(quizConfig.languageCode)
}
