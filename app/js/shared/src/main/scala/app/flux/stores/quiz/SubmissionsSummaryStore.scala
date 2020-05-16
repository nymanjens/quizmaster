package app.flux.stores.quiz

import java.time.Duration

import hydro.common.time.JavaTimeImplicits._
import app.api.ScalaJsApi.TeamOrQuizStateUpdate._
import app.api.ScalaJsApiClient
import app.flux.action.AppActions
import app.flux.stores.quiz.SubmissionsSummaryStore.QuestionIndex
import app.flux.stores.quiz.SubmissionsSummaryStore.State
import app.models.access.ModelFields
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.SubmissionEntity
import app.models.user.User
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.common.SerializingTaskQueue
import hydro.flux.action.Dispatcher
import hydro.flux.stores.AsyncEntityDerivedStateStore
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class SubmissionsSummaryStore(
    implicit entityAccess: JsEntityAccess,
    i18n: I18n,
    user: User,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
    scalaJsApiClient: ScalaJsApiClient,
) extends AsyncEntityDerivedStateStore[State] {

  private val firstQuestionDurationEstimate: Duration = Duration.ofMinutes(1)

  // **************** Implementation of AsyncEntityDerivedStateStore methods **************** //
  override protected def calculateState(): Future[State] = async {
    val allSubmissions =
      await(entityAccess.newQuery[SubmissionEntity]().data()).sortBy(s => (s.roundIndex, s.createTime))

    State(
      latestSubmissionsMap = allSubmissions
        .groupBy(e => QuestionIndex(roundIndex = e.roundIndex, questionIndex = e.questionIndex))
        .mapValues { submissions =>
          submissions.groupBy(_.teamId).mapValues(_.last)
        },
      roundToTimeEstimateMap = (
        for (roundIndex <- quizConfig.rounds.indices)
          yield
            roundIndex -> {
              if (allSubmissions.exists(_.roundIndex == roundIndex)) {
                val previousRoundEnd = allSubmissions.reverseIterator.find(s => s.roundIndex < roundIndex)
                val firstQuestionInRound = allSubmissions.find(_.roundIndex == roundIndex).get
                val lastQuestionInRound = allSubmissions.reverseIterator.find(_.roundIndex == roundIndex).get
                val startTime = previousRoundEnd
                  .map(_.createTime) getOrElse (firstQuestionInRound.createTime - firstQuestionDurationEstimate)

                lastQuestionInRound.createTime - startTime
              } else {
                Duration.ZERO
              }
            }
      ).toMap.withDefault(_ => Duration.ZERO),
      totalQuizTimeEstimate = if (allSubmissions.nonEmpty) {
        firstQuestionDurationEstimate + (allSubmissions.last.createTime - allSubmissions.head.createTime)
      } else Duration.ZERO
    )
  }

  override protected def modificationImpactsState(
      entityModification: EntityModification,
      state: State,
  ): Boolean = {
    entityModification.entityType == SubmissionEntity.Type
  }

  // **************** Additional public API: Read methods **************** //
  def stateOrEmpty: State = state getOrElse State.nullInstance
}

object SubmissionsSummaryStore {
  case class State(
      latestSubmissionsMap: Map[QuestionIndex, Map[TeamId, SubmissionEntity]],
      roundToTimeEstimateMap: Map[RoundIndex, Duration],
      totalQuizTimeEstimate: Duration,
  ) {
    def points(roundIndex: Int, questionIndex: Int, teamId: Long)(implicit quizConfig: QuizConfig): Int = {
      if (hasAnySubmission(roundIndex, questionIndex, teamId)) {
        val allSubmissionsForQuestion = latestSubmissionsMap(QuestionIndex(roundIndex, questionIndex))
        val submissionEntity: SubmissionEntity = allSubmissionsForQuestion(teamId)
        val maybeFirstCorrectSubmission: Option[SubmissionEntity] =
          allSubmissionsForQuestion.values.toVector
            .sortBy(_.createTime)
            .find(_.isCorrectAnswer == Some(true))
        val question = quizConfig.rounds(roundIndex).questions(questionIndex)

        submissionEntity.isCorrectAnswer match {
          case Some(true) =>
            if (maybeFirstCorrectSubmission == Some(submissionEntity)) {
              question.pointsToGainOnFirstAnswer
            } else {
              question.pointsToGain
            }
          case Some(false) => question.pointsToGainOnWrongAnswer
          case None        => 0
        }
      } else {
        0
      }
    }

    def totalPoints(team: Team)(implicit quizConfig: QuizConfig): Int = {
      {
        for (QuestionIndex(roundIndex, questionIndex) <- latestSubmissionsMap.keysIterator)
          yield points(roundIndex, questionIndex, team.id)
      }.sum
    }

    def hasAnySubmission(roundIndex: Int, questionIndex: Int, teamId: Long): Boolean = {
      val maybeSubmission: Option[SubmissionEntity] =
        for {
          teamMap <- latestSubmissionsMap.get(QuestionIndex(roundIndex, questionIndex))
          submission <- teamMap.get(teamId)
        } yield submission

      maybeSubmission.isDefined
    }
  }
  case object State {
    val nullInstance = State(
      latestSubmissionsMap = Map(),
      roundToTimeEstimateMap = Map().withDefault(_ => Duration.ZERO),
      totalQuizTimeEstimate = Duration.ZERO,
    )
  }

  type TeamId = Long
  type RoundIndex = Int
  case class QuestionIndex(
      roundIndex: Int,
      questionIndex: Int,
  )
}
