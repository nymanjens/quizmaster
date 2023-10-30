package app.models.quiz.config

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import app.common.QuizAssets
import hydro.common.ResourceFiles
import com.google.common.base.Throwables
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import hydro.common.Annotations.visibleForTesting
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import play.api.Logger

final class ConfigModule(exitOnFailure: Boolean = true) extends AbstractModule {

  val logger: Logger = Logger(this.getClass())

  override def configure() = {}

  @Provides()
  @Singleton()
  private[config] def config(
      playConfiguration: play.api.Configuration,
      quizAssets: QuizAssets,
      quizConfigParsableValue: QuizConfigParsableValue,
  ): QuizConfig = {
    var configLocation = playConfiguration.get[String]("app.quiz.configYamlFilePath")

    try {
      // Canoicalization may throw an error if something goes wrong, so has to be inside the try-catch
      configLocation = ResourceFiles.canonicalizePath(configLocation)

      // get data
      val stringData = {
        require(Files.exists(Paths.get(configLocation)), s"Could not find $configLocation as file")
        scala.io.Source.fromFile(configLocation).mkString
      }

      // parse data
      ValidatingYamlParser.parse(stringData, quizConfigParsableValue)
    } catch {
      case e: Throwable =>
        val stackTrace = Throwables.getStackTraceAsString(e)
        logger.error(s"Error when parsing ${configLocation}:\n\n$stackTrace")
        // Make error output less noisy by shutting down early (instead of 20+ Guice exceptions while injecting QuizConfig)
        if (exitOnFailure) {
          // Sleep for 1 second so that Logger.error() has time to write the error message
          println(s"\n\n  Will exit in 1 second due to a failure in ${configLocation}\n\n")
          Thread.sleep(1000)

          new File("RUNNING_PID").delete()
          System.exit(1)
        }
        throw new RuntimeException(s"Error when parsing ${configLocation}", e)
    }
  }
}
