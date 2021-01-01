package app.common

import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import com.google.common.base.CharMatcher
import com.google.common.hash
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.common.io.ByteStreams
import com.google.inject._
import hydro.common.CollectionUtils.conditionalOption
import hydro.common.GuavaReplacement.Iterables.getOnlyElement
import hydro.common.GuavaReplacement.Splitter
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

  def toFullPath(source: String): Path = {
    fullPathOrValidationError(source).get
  }

  def assetExistsOrValidationError(source: String): Option[String] = {
    fullPathOrValidationError(source) match {
      case Success(_)         => None
      case Failure(exception) => Some(exception.getMessage)
    }
  }

  private def fullPathOrValidationError(source: String): Try[Path] = {
    if (source.toLowerCase.startsWith("http://") || source.toLowerCase.startsWith("https://")) {
      fullPathOrValidationErrorFromUrl(source)
    } else {
      fullPathOrValidationErrorFromRelativePath(source)
    }
  }

  private def fullPathOrValidationErrorFromRelativePath(relativePath: String): Try[Path] = {
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
      case _ =>
        Failure(new RuntimeException(s"Found multiple paths with the same resource: $existingPaths"))
    }
  }

  private def fullPathOrValidationErrorFromUrl(url: String): Try[Path] = {
    val hashed = Hashing.sha256().hashString(url, StandardCharsets.UTF_8).toString
    val extension = Splitter.on('.').omitEmptyStrings().trimResults().split(url).lastOption getOrElse ""
    val filename =
      if (extension.length <= 4 && CharMatcher.javaLetterOrDigit().matchesAllOf(extension))
        s"$hashed.$extension"
      else hashed
    val filePath = configPath.resolve("assets_cache").resolve(filename)

    Try {
      if (!Files.exists(configPath.resolve("assets_cache"))) {
        Files.createDirectory(configPath.resolve("assets_cache"))
      }
      if (!Files.exists(filePath)) {
        Files.write(filePath, makeHttpGet(url))
      }
      filePath
    }
  }

  private def makeHttpGet(url: String): Array[Byte] = {
    var inputStream: InputStream = null
    try {
      val urlConnection = new URL(url).openConnection()
      urlConnection.setRequestProperty("User-Agent", "Quizmaster")
      inputStream = urlConnection.getInputStream()
      ByteStreams.toByteArray(inputStream)
    } finally {
      if (inputStream != null) {
        inputStream.close()
      }
    }
  }
}
