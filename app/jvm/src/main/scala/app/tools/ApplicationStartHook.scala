package app.tools

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

import app.common.FixedPointNumber
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.quiz.Team
import app.models.quiz.export.ExportImport
import app.models.quiz.export.ExportImport.FullState
import app.models.quiz.QuizState
import app.models.user.User.onlyUser
import com.google.inject.Inject
import hydro.common.ResourceFiles
import hydro.common.time.Clock
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import play.api.Application

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
    if (AppConfigHelper.loadDummyData) {
      loadDummyData()
    }

    startExportingChanges()
  }

  private def processFlags(): Unit = {}

  private def loadDummyData(): Unit = {
    entityAccess.persistEntityModifications(
      EntityModification.createAddWithRandomId(
        Team(
          name = "Team A",
          score = FixedPointNumber(0),
          index = 0,
        ),
      ),
      EntityModification.createAddWithRandomId(
        Team(
          name = "Team B",
          score = FixedPointNumber(0),
          index = 1,
        ),
      ),
      EntityModification.createAddWithRandomId(
        Team(
          name = "Team C",
          score = FixedPointNumber(0),
          index = 2,
        ),
      ),
      EntityModification.createAddWithRandomId(
        Team(
          name = "Team D",
          score = FixedPointNumber(0),
          index = 3,
        ),
      ),
    )
  }

  private def startExportingChanges(): Unit = {
    var lastExportedString = ""

    val task = new Runnable {
      def run(): Unit = {
        val fullState = FullState(
          teams = entityAccess
            .newQuerySync[Team]()
            .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
            .data(),
          quizState = entityAccess
            .newQuerySync[QuizState]()
            .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId) getOrElse QuizState.nullInstance,
        )
        val newExportedString = ExportImport.exportToString(fullState)

        if (lastExportedString != newExportedString) {
          println("Periodic backup, so you don't lose any data (to paste in master setup page):")
          println(newExportedString)
        }
        lastExportedString = newExportedString
      }
    }

    new ScheduledThreadPoolExecutor( /* corePoolSize = */ 1)
      .scheduleAtFixedRate(task, /* initialDelay = */ 1, /* period = */ 10, TimeUnit.SECONDS)
  }

  private def assertExists(path: Path): Path = {
    require(ResourceFiles.exists(path), s"Couldn't find path: $path")
    path
  }

  private object CommandLineFlags {
    private val properties = System.getProperties.asScala

    private def getBoolean(name: String): Boolean = properties.get(name).isDefined

    private def getExistingPath(name: String): Option[Path] =
      properties.get(name) map (Paths.get(_)) map assertExists
  }

  private object AppConfigHelper {
    def loadDummyData: Boolean = getBoolean("app.development.loadDummyData")

    private def getBoolean(cfgPath: String): Boolean =
      app.configuration.getOptional[Boolean](cfgPath) getOrElse false

    private def getString(cfgPath: String): Option[String] =
      app.configuration.getOptional[String](cfgPath)

    private def getExistingPath(cfgPath: String): Path = assertExists {
      Paths.get(app.configuration.get[String](cfgPath))
    }
  }
}
