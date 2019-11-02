package app.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import com.google.inject._
import hydro.common.ResourceFiles

@Singleton
final class QuizAssets @Inject()(
    implicit playConfiguration: play.api.Configuration,
    quizConfig: QuizConfig,
) {
  validateThatAssetsExist(quizConfig)

  def quizImage(file: String): Path = {
    assertExists {
      configPath.resolve("images").resolve(file)
    }
  }

  def quizAudio(file: String): Path = {
    assertExists {
      configPath.resolve("audio").resolve(file)
    }
  }

  private lazy val configPath: Path = {
    val configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")
    Paths.get(ResourceFiles.canonicalizePath(configLocation)).getParent
  }

  private def validateThatAssetsExist(quizConfig: QuizConfig): Unit = {
    def validateImage(maybeImage: Option[Image]): Unit = {
      if (maybeImage.isDefined) assertExists(quizImage(maybeImage.get.src))
    }
    def validateAudio(maybeAudio: Option[String]): Unit = {
      if (maybeAudio.isDefined) assertExists(quizAudio(maybeAudio.get))
    }

    for {
      round <- quizConfig.rounds
      question <- round.questions
    } {
      question match {
        case single: Question.Single =>
          validateImage(single.image)
          validateImage(single.answerImage)
          validateAudio(single.audioSrc)
        case double: Question.Double =>
      }
    }
  }

  private def assertExists(path: Path): Path = {
    require(Files.exists(path), s"Could not find path $path")
    path
  }
}
