package app.models.quiz.export

import app.common.FixedPointNumber
import app.models.quiz.export.ExportImport.FullState
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.TimerState
import app.models.quiz.Team
import org.junit.runner._
import org.specs2.mutable.Specification
import org.specs2.runner._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ExportImportTest extends Specification {

  "importFromString(exportToString())" in {
    val fullState = FullState(
      teams = Seq(
        Team(
          name = "Team A",
          score = FixedPointNumber(1.2),
          index = 0,
        ),
        Team(
          name = "Team B",
          score = FixedPointNumber(-2.2),
          index = 1,
        ),
      ),
      quizState = QuizState(
        roundIndex = -2,
        questionIndex = -2,
        questionProgressIndex = 0,
        timerState = TimerState.nullInstance,
        submissions = Seq(),
        imageIsEnlarged = false,
        generalQuizSettings = GeneralQuizSettings(
          sortTeamsByScore = true,
          showAnswers = false,
          answerBulletType = AnswerBulletType.Characters,
        ),
      ),
    )

    ExportImport.importFromString(ExportImport.exportToString(fullState)) mustEqual fullState
  }
}
