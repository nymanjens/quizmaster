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
    def relativeImagePath(maybeImage: Option[Image]): Option[Path] = {
      maybeImage.map(i => Paths.get("images").resolve(i.src))
    }
    def relativeAudioPath(maybeAudio: Option[String]): Option[Path] = {
      maybeAudio.map(a => Paths.get("audio").resolve(a))
    }

    val missingRelativePaths =
      for {
        round <- quizConfig.rounds
        question <- round.questions
        relativePath <- {
          question match {
            case single: Question.Single =>
              Seq() ++
                relativeImagePath(single.image) ++
                relativeImagePath(single.answerImage) ++
                relativeAudioPath(single.audioSrc)
            case double: Question.Double => Seq()
          }
        }
        if !Files.exists(configPath.resolve(relativePath))
      } yield relativePath

    require(
      missingRelativePaths.isEmpty,
      s"""Could not find the following assets:
        |
        |${missingRelativePaths.sorted.map(p => s"  - $p").mkString("\n")}
        |
        |""".stripMargin)
  }

  private def assertExists(path: Path): Path = {
    require(Files.exists(path), s"Could not find path $path")
    path
  }
}
