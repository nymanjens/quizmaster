package app.api

import hydro.models.access.DbQueryImplicits._
import java.util.concurrent.Executors

import app.api.ScalaJsApi._
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.Submission
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.QuizState.TimerState
import app.models.quiz.Team
import app.models.user.User
import com.google.inject._
import hydro.api.PicklableDbQuery
import hydro.common.PlayI18n
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.time.Clock
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.util.Random

@Singleton
final class ScalaJsApiServerFactory @Inject()(
    implicit clock: Clock,
    entityAccess: JvmEntityAccess,
    i18n: PlayI18n,
    quizConfig: QuizConfig,
    playConfiguration: play.api.Configuration,
) {

  private val singleThreadedExecutor =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  def create()(implicit user: User): ScalaJsApi = new ScalaJsApi() {

    override def getInitialData() =
      GetInitialDataResponse(
        i18nMessages = i18n.allI18nMessages,
        nextUpdateToken = toUpdateToken(clock.nowInstant),
        quizConfig = quizConfig,
        masterSecret = playConfiguration.get[String]("app.quiz.master-secret"),
      )

    override def getAllEntities(types: Seq[EntityType.any]) = {
      // All modifications are idempotent so we can use the time when we started getting the entities as next update token.
      val nextUpdateToken: UpdateToken = toUpdateToken(clock.nowInstant)
      val entitiesMap: Map[EntityType.any, Seq[Entity]] = {
        types
          .map(entityType => {
            entityType -> entityAccess.newQuerySync()(entityType).data()
          })
          .toMap
      }

      GetAllEntitiesResponse(entitiesMap, nextUpdateToken)
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]): Unit = {
      entityAccess.persistEntityModifications(modifications)
    }

    override def executeDataQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].data(query)
      }
      internal
    }

    override def executeCountQuery(dbQuery: PicklableDbQuery) = {
      def internal[E <: Entity] = {
        val query = dbQuery.toRegular.asInstanceOf[DbQuery[E]]
        implicit val entityType = query.entityType.asInstanceOf[EntityType[E]]
        entityAccess.queryExecutor[E].count(query)
      }
      internal
    }

    override def addSubmission(teamId: Long, submissionValue: Submission.SubmissionValue): Unit = {
      singleThreadedExecutor.execute(() => {
        implicit val quizState =
          entityAccess
            .newQuerySync[QuizState]()
            .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId) getOrElse QuizState.nullInstance
        val allTeams =
          entityAccess
            .newQuerySync[Team]()
            .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
            .data()
        val team = allTeams.find(_.id == teamId).get

        if (quizState.canSubmitResponse(team)) {
          val question = quizState.maybeQuestion.get
          def teamHasSubmission(thisTeam: Team): Boolean =
            quizState.submissions.exists(_.teamId == thisTeam.id)
          lazy val allOtherTeamsHaveSubmission = allTeams.filter(_ != team).forall(teamHasSubmission)

          if (question.isMultipleChoice) {
            addVerifiedSubmission(
              Submission(teamId = team.id, submissionValue),
              resetTimer = question.isInstanceOf[Question.Double],
              pauseTimer =
                if (question.onlyFirstGainsPoints) question.isCorrectAnswer(submissionValue)
                else allOtherTeamsHaveSubmission,
              allowMoreThanOneSubmissionPerTeam = false,
              removeEarlierDifferentSubmissionBySameTeam = !question.onlyFirstGainsPoints,
            )
          } else { // Not multiple choice
            addVerifiedSubmission(
              Submission(teamId = team.id, submissionValue),
              pauseTimer = if (question.onlyFirstGainsPoints) true else allOtherTeamsHaveSubmission,
              allowMoreThanOneSubmissionPerTeam = question.onlyFirstGainsPoints,
              removeEarlierDifferentSubmissionBySameTeam = !question.onlyFirstGainsPoints,
            )
          }
        }
      })
    }

    private def addVerifiedSubmission(
        submission: Submission,
        resetTimer: Boolean = false,
        pauseTimer: Boolean = false,
        allowMoreThanOneSubmissionPerTeam: Boolean,
        removeEarlierDifferentSubmissionBySameTeam: Boolean = false,
    )(implicit quizState: QuizState): Unit = {

      val updatedState = {
        val oldSubmissions = quizState.submissions
        val newSubmissions = {
          val filteredOldSubmissions = {
            if (removeEarlierDifferentSubmissionBySameTeam) {
              def differentSubmissionBySameTeam(s: Submission): Boolean = {
                s.teamId == submission.teamId && s.value != submission.value
              }
              oldSubmissions.filterNot(differentSubmissionBySameTeam)
            } else {
              oldSubmissions
            }
          }

          val submissionAlreadyExists = filteredOldSubmissions.exists(_.teamId == submission.teamId)

          if (submissionAlreadyExists && !allowMoreThanOneSubmissionPerTeam) {
            filteredOldSubmissions
          } else {
            filteredOldSubmissions :+ submission
          }
        }

        if (oldSubmissions == newSubmissions) {
          // Don't change timerState if there were no submissions, the return value is always true (incorrectly)
          quizState
        } else {
          quizState.copy(
            timerState =
              if (resetTimer) TimerState.createStarted()
              else if (pauseTimer)
                TimerState(
                  lastSnapshotInstant = clock.nowInstant,
                  lastSnapshotElapsedTime = quizState.timerState.elapsedTime(),
                  timerRunning = false,
                )
              else quizState.timerState,
            submissions = newSubmissions,
          )
        }
      }

      entityAccess.persistEntityModifications(
        EntityModification.createUpdate(
          updatedState,
          Seq(ModelFields.QuizState.timerState, ModelFields.QuizState.submissions),
        ))
    }
  }
}
