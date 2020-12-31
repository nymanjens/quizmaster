package app.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import com.google.inject._
import hydro.common.CollectionUtils.conditionalOption
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import hydro.common.ResourceFiles

import scala.util.Failure
import scala.util.Success
import scala.util.Try

@Singleton
final class QuizAssets @Inject() (implicit
    playConfiguration: play.api.Configuration
) {

  lazy val configPath: Path = {
    val configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")
    Paths.get(ResourceFiles.canonicalizePath(configLocation)).getParent
  }

  def toFullPath(relativePath: String): Path = {
    fullPathOrValidationError(relativePath).get
  }

  def assetExistsOrValidationError(relativePath: String): Option[String] = {
    fullPathOrValidationError(relativePath) match {
      case Success(_)         => None
      case Failure(exception) => Some(exception.getMessage)
    }
  }

  private def fullPathOrValidationError(relativePath: String): Try[Path] = {
    val existingPaths =
      for {
        assetFolder <- Seq("assets", "images", "audio", "video")
        fullPath <- Some(configPath.resolve(assetFolder).resolve(relativePath))
        if Files.exists(fullPath)
      } yield fullPath

    existingPaths match {
      case Seq() =>
        Failure(
          new RuntimeException(s"Could not find path ${configPath.resolve("assets").resolve(relativePath)}")
        )
      case Seq(p) => Success(p)
      case _      => Failure(new RuntimeException(s"Found multiple paths with the same resource: $existingPaths"))
    }
  }
}
