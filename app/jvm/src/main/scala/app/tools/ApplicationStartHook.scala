package app.tools

import java.nio.file.Path
import java.nio.file.Paths

import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.ResourceFiles
import hydro.common.time.Clock
import play.api.Application
import play.api.Mode

import scala.collection.JavaConverters._

final class ApplicationStartHook @Inject()(
    implicit app: Application,
    entityAccess: JvmEntityAccess,
    clock: Clock,
) {
  onStart()

  private def onStart(): Unit = {
    processFlags()

    // Populate the database with dummy data
    if (app.mode == Mode.Test || app.mode == Mode.Dev) {
      if (AppConfigHelper.loadDummyData) {
        loadDummyData()
      }
    }
  }

  private def processFlags(): Unit = {}

  private def loadDummyData(): Unit = {
    // TODO: Implement
  }

  private def assertExists(path: Path): Path = {
    require(ResourceFiles.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    def dropAndCreateNewDb: Boolean = getBoolean("dropAndCreateNewDb")
    def createAdminUser: Boolean = getBoolean("createAdminUser")

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def dropAndCreateNewDb: Boolean = getBoolean("app.development.dropAndCreateNewDb")
    def loadDummyUsers: Boolean = getBoolean("app.development.loadDummyUsers")
    def loadDummyData: Boolean = getBoolean("app.development.loadDummyData")
    def defaultPassword: Option[String] = getString("app.setup.defaultPassword")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getOptional[Boolean](cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getOptional[String](cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.get[String](cfgPath))
    }
  }
}