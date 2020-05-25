package app.models.quiz.config

import java.nio.file.Files
import java.nio.file.Paths

import app.common.QuizAssets
import hydro.common.ResourceFiles
import com.google.common.base.Throwables
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.introspector.BeanAccess
import play.api.Logger

final class ConfigModule extends AbstractModule {

  override def configure() = {}

  @Provides()
  @Singleton()
  private[config] def config(
      playConfiguration: play.api.Configuration,
      quizAssets: QuizAssets,
  ): QuizConfig = {
    // get configLocation
    val configLocation =
      ResourceFiles.canonicalizePath(playConfiguration.get[String]("app.quiz.configYamlFilePath"))

    try {
      // get data
      val stringData = {
        if (Files.exists(Paths.get(configLocation))) {
          scala.io.Source.fromFile(configLocation).mkString
        } else {
          require(
            ResourceFiles.exists(configLocation),
            s"Could not find $configLocation as file or as resource")
          ResourceFiles.read(configLocation)
        }
      }

      // parse data
      val constr = new CustomClassLoaderConstructor(getClass.getClassLoader)
      val yaml = new Yaml(constr)
      yaml.setBeanAccess(BeanAccess.FIELD)
      val configData = yaml.load(stringData).asInstanceOf[ParsableQuizConfig]

      // convert to parsed config
      val quizConfig = configData.parse
      quizAssets.validateThatAssetsExist(quizConfig)
      quizConfig
    } catch {
      case e: Throwable =>
        val stackTrace = Throwables.getStackTraceAsString(e)
        Logger.error(s"Error when parsing ${configLocation}:\n\n$stackTrace")
        // Make error output less noisy by shutting down early (instead of 20+ Guice exceptions while injecting QuizConfig)
        System.exit(1)
        throw e
    }
  }
}
