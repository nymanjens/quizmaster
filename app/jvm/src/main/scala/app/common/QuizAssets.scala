package app.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Image
import app.models.quiz.config.QuizConfig.Question
import com.google.inject._
import hydro.common.CollectionUtils.conditionalOption
import hydro.common.ResourceFiles

@Singleton
final class QuizAssets @Inject() (implicit
    playConfiguration: play.api.Configuration
) {

  lazy val configPath: Path = {
    val configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")
    Paths.get(ResourceFiles.canonicalizePath(configLocation)).getParent
  }

  def quizImage(relativePath: String): Path = {
    assertExists {
      configPath.resolve("images").resolve(relativePath)
    }
  }

  def quizAudio(relativePath: String): Path = {
    assertExists {
      configPath.resolve("audio").resolve(relativePath)
    }
  }

  def quizVideo(relativePath: String): Path = {
    assertExists {
      configPath.resolve("video").resolve(relativePath)
    }
  }

  def imageExistsOrValidationError(relativePath: String): Option[String] = {
    assetExistsOrValidationError(Paths.get("images").resolve(relativePath))
  }

  def audioExistsOrValidationError(relativePath: String): Option[String] = {
    assetExistsOrValidationError(Paths.get("audio").resolve(relativePath))
  }

  def videoExistsOrValidationError(relativePath: String): Option[String] = {
    assetExistsOrValidationError(Paths.get("video").resolve(relativePath))
  }

  private def assetExistsOrValidationError(relativePath: Path): Option[String] = {
    conditionalOption(
      !Files.exists(configPath.resolve(relativePath)),
      s"Could not find the asset: $relativePath",
    )
  }

  private def assertExists(path: Path): Path = {
    require(Files.exists(path), s"Could not find path $path")
    path
  }
}
