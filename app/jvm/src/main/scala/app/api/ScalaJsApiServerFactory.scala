package app.api

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Math.abs
import java.net.URL
import java.net.URLEncoder

import hydro.common.time.JavaTimeImplicits._
import java.time.Duration
import java.util.concurrent.Executors

import app.api.ScalaJsApi._
import app.api.ScalaJsApi.TeamOrQuizStateUpdate._
import app.common.FixedPointNumber
import app.common.FixedPointNumber
import app.models.access.JvmEntityAccess
import app.models.access.ModelFields
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState.Submission
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState
import app.models.quiz.QuizState.TimerState
import app.models.quiz.Team
import app.models.quiz.export.ExportImport
import app.models.quiz.export.ExportImport.FullState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.SubmissionEntity
import app.models.user.User
import app.AppVersion
import com.google.inject._
import hydro.api.PicklableDbQuery
import hydro.common.PlayI18n
import hydro.common.UpdateTokens.toUpdateToken
import hydro.common.time.Clock
import hydro.common.CollectionUtils.maybeGet
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbQuery
import hydro.models.access.DbQuery.Sorting

import scala.collection.immutable.ListMap
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.BufferedSource
import scala.io.Source
import scala.math.Ordering
import scala.util.Random

@Singleton
final class ScalaJsApiServerFactory @Inject() (implicit
    clock: Clock,
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

    override def doTeamOrQuizStateUpdate(teamOrQuizStateUpdate: TeamOrQuizStateUpdate): Unit = {
      executeInSingleThreadAndWait {
        teamOrQuizStateUpdate match {
          case ReplaceAllEntitiesByImportString(importString: String) =>
            val FullState(teamsToImport, quizStateToImport) = ExportImport.importFromString(importString)
            println(s"  Importing: teams = ${teamsToImport}, quizState = ${quizStateToImport}")

            val deleteExistingTeams = fetchAllTeams().map(team => EntityModification.createRemove(team))
            val importTeams = teamsToImport.map(team => EntityModification.createAddWithRandomId(team))
            val addOrUpdateQuizState = Seq(
              EntityModification.Add(quizStateToImport),
              EntityModification.createUpdateAllFields(quizStateToImport),
            )
            entityAccess.persistEntityModifications(
              deleteExistingTeams ++ importTeams ++ addOrUpdateQuizState
            )

          case MaybeAddTeam(uniqueName) =>
            val teams = fetchAllTeams()

            if (!teams.exists(t => Team.areEquivalentTeamNames(t.name, uniqueName))) {
              val maxIndex = if (teams.nonEmpty) teams.map(_.index).max else -1
              val modification = EntityModification.createAddWithRandomId(
                Team(
                  name = uniqueName,
                  score = FixedPointNumber(0),
                  index = maxIndex + 1,
                )
              )
              entityAccess.persistEntityModifications(modification)
            }

          case MaybeUpdateTeamName(teamId: Long, newName: String) =>
            val teams = fetchAllTeams()

            if (!teams.filter(_.id != teamId).exists(t => Team.areEquivalentTeamNames(t.name, newName))) {
              val team = teams.find(_.id == teamId).get
              entityAccess.persistEntityModifications(
                EntityModification.createUpdateAllFields(team.copy(name = newName))
              )
            }

          case UpdateScore(teamId: Long, scoreDiff: FixedPointNumber) =>
            updateScore(teamId, scoreDiff)

          case DeleteTeam(teamId: Long) =>
            for {
              team <- fetchAllTeams()
              if team.id == teamId
            } entityAccess.persistEntityModifications(EntityModification.createRemove(team))

          case GoToPreviousStep() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToPreviousStepUpdate)

          case GoToNextStep() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToNextStepUpdate)

          case GoToPreviousQuestion() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToPreviousQuestionUpdate)

          case GoToNextQuestion() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToNextQuestionUpdate)

          case GoToPreviousRound() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToPreviousRoundUpdate)
          case GoToNextRound() =>
            StateUpsertHelper.doQuizStateUpsert(StateUpsertHelper.goToNextRoundUpdate)

          case ResetCurrentQuestion() =>
            val oldQuizState = fetchQuizState()
            StateUpsertHelper.doQuizStateUpsert(
              _.copy(timerState = TimerState.createStarted(), submissions = Seq())
            )
            entityAccess.persistEntityModifications(
              oldQuizState.submissions.map(s => EntityModification.Remove[SubmissionEntity](s.id))
            )

          case ToggleImageIsEnlarged() =>
            StateUpsertHelper.doQuizStateUpsert { oldState =>
              oldState.copy(imageIsEnlarged = !oldState.imageIsEnlarged)
            }

          case SetSortTeamsByScore(sortTeamsByScore: Boolean) =>
            StateUpsertHelper.doQuizStateUpsert { oldState =>
              oldState.copy(
                generalQuizSettings = oldState.generalQuizSettings.copy(sortTeamsByScore = sortTeamsByScore)
              )
            }

          case SetShowAnswers(showAnswers: Boolean) =>
            StateUpsertHelper.doQuizStateUpsert { oldState =>
              oldState.copy(
                generalQuizSettings = oldState.generalQuizSettings.copy(showAnswers = showAnswers)
              )
            }

          case SetAnswerBulletType(answerBulletType: AnswerBulletType) =>
            StateUpsertHelper.doQuizStateUpsert { oldState =>
              oldState.copy(
                generalQuizSettings = oldState.generalQuizSettings.copy(answerBulletType = answerBulletType)
              )
            }

          case ToggleTimerPaused(timerRunningValue: Option[Boolean]) =>
            StateUpsertHelper.doQuizStateUpsert { state =>
              val timerState = state.timerState
              state.copy(
                timerState = TimerState(
                  lastSnapshotInstant = clock.nowInstant,
                  lastSnapshotElapsedTime = timerState.elapsedTime(),
                  timerRunning = timerRunningValue getOrElse (!timerState.timerRunning),
                  uniqueIdOfMediaPlaying = timerState.uniqueIdOfMediaPlaying,
                )
              )
            }

          case AddTimeToTimer(duration: Duration) =>
            StateUpsertHelper.doQuizStateUpsert { state =>
              val maxTime = state.maybeQuestion.map(_.maxTime) getOrElse Duration.ZERO
              val timerState = state.timerState
              val durationIsPositive = !duration.isNegative
              if (timerState.hasFinished(maxTime) && durationIsPositive) {
                // Adding time to an expired timer --> reset because:
                //   - the elapsed time may not be accurate
                //   - the timer should start running again
                state.copy(
                  timerState = TimerState(
                    lastSnapshotInstant = clock.nowInstant,
                    lastSnapshotElapsedTime = maxTime - duration,
                    timerRunning = true,
                    uniqueIdOfMediaPlaying = timerState.uniqueIdOfMediaPlaying,
                  )
                )
              } else {
                state.copy(
                  timerState = TimerState(
                    lastSnapshotInstant = clock.nowInstant,
                    lastSnapshotElapsedTime = Seq(Duration.ZERO, timerState.elapsedTime() - duration).max,
                    timerRunning = timerState.timerRunning,
                    uniqueIdOfMediaPlaying = timerState.uniqueIdOfMediaPlaying,
                  )
                )
              }
            }

          case RestartMedia() =>
            StateUpsertHelper.doQuizStateUpsert { state =>
              state.copy(
                timerState = state.timerState.copy(uniqueIdOfMediaPlaying = abs(Random.nextLong))
              )
            }

          case AddSubmission(teamId: Long, submissionValue: SubmissionValue) =>
            addSubmission(teamId, submissionValue)

          case SetSubmissionCorrectness(submissionId: Long, isCorrectAnswer: Boolean) =>
            updateSubmissionInStateAndEntity(submissionId) { oldSubmission =>
              // Reset points as well because its old value is likely moot
              val points =
                fetchQuizState().pointsToGainBySubmission(
                  isCorrectAnswer = Some(isCorrectAnswer),
                  submissionId = submissionId,
                  submissionValue = oldSubmission.value,
                )

              if (oldSubmission.scored) {
                // If scoring already happened, the current team score has to be updated
                updateScore(oldSubmission.teamId, scoreDiff = points - oldSubmission.points)
              }

              val newSubmissionValue = oldSubmission.value match {
                case SubmissionValue.MultipleTextAnswers(answers) =>
                  SubmissionValue.MultipleTextAnswers(answers.map(_.copy(isCorrectAnswer = isCorrectAnswer)))
                case oldValue => oldValue
              }

              oldSubmission.copy(
                value = newSubmissionValue,
                isCorrectAnswer = Some(isCorrectAnswer),
                points = points,
              )
            }

          case SetMultiAnswerCorrectness(submissionId: Long, answerIndex: Int, isCorrectAnswer: Boolean) =>
            updateSubmissionInStateAndEntity(submissionId) { oldSubmission =>
              val newSubmissionValue = oldSubmission.value match {
                case SubmissionValue.MultipleTextAnswers(answers) =>
                  val newAnswer = answers(answerIndex).copy(isCorrectAnswer = isCorrectAnswer)
                  SubmissionValue.MultipleTextAnswers(answers.updated(answerIndex, newAnswer))
              }

              val points =
                fetchQuizState().pointsToGainBySubmission(
                  isCorrectAnswer = None,
                  submissionId = submissionId,
                  submissionValue = newSubmissionValue,
                )

              val pointsForCorrectAnswer =
                oldSubmission.question.defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer = false)
              val isCorrectGlobalAnswer = points match {
                case _ if points <= 0                     => Some(false)
                case _ if points < pointsForCorrectAnswer => None
                case _                                    => Some(true)
              }

              if (oldSubmission.scored) {
                // If scoring already happened, the current team score has to be updated
                updateScore(oldSubmission.teamId, scoreDiff = points - oldSubmission.points)
              }

              oldSubmission.copy(
                value = newSubmissionValue,
                isCorrectAnswer = isCorrectGlobalAnswer,
                points = points,
              )
            }

          case SetSubmissionPoints(submissionId: Long, points: FixedPointNumber) =>
            updateSubmissionInStateAndEntity(submissionId) { oldSubmission =>
              val pointsForCorrectAnswer =
                oldSubmission.question.defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer = false)
              val isCorrectAnswer = points match {
                case _ if points <= 0                     => Some(false)
                case _ if points < pointsForCorrectAnswer => None
                case _                                    => Some(true)
              }

              if (oldSubmission.scored) {
                // If scoring already happened, the current team score has to be updated
                updateScore(oldSubmission.teamId, scoreDiff = points - oldSubmission.points)
              }

              oldSubmission.copy(isCorrectAnswer = isCorrectAnswer, points = points)
            }
        }
      }
    }

    private def addSubmission(teamId: Long, submissionValue: Submission.SubmissionValue): Unit = {
      implicit val quizState = fetchQuizState()
      val allTeams = fetchAllTeams()
      val team = allTeams.find(_.id == teamId).get

      val isDuplicate = {
        // Bugfix: Two consecutive buttonpresses may have the second one result in an error while its behavior
        // should be a no-op.

        val duplicateSubmissions =
          for {
            submission <- quizState.submissions
            if submission.teamId == teamId && submission.value == submissionValue
            submissionEntity <- Some(entityAccess.newQuerySync[SubmissionEntity]().findById(submission.id))
            if submissionEntity.createTime > (clock.nowInstant - Duration.ofSeconds(2))
          } yield submissionEntity

        duplicateSubmissions.nonEmpty
      }

      if (isDuplicate) {
        // Do nothing
      } else {
        require(quizState.canSubmitResponse(team), "Responses are closed")

        val question = quizState.maybeQuestion.get
        val isCorrectAnswer = question.isCorrectAnswer(submissionValue)
        val shouldPauseTimerBecauseAllTeamsHaveSubmission = {
          def teamHasSubmission(thisTeam: Team): Boolean =
            quizState.submissions.exists(_.teamId == thisTeam.id)
          val allOtherTeamsHaveSubmission = allTeams.filter(_ != team).forall(teamHasSubmission)
          // Only pause if this submission is the one that changes the state from "not all teams have
          // submitted" to "all teams have submitted". This means that this team shouldn't already have a
          // submission.
          allOtherTeamsHaveSubmission && !teamHasSubmission(team)
        }

        if (question.isMultipleChoice) {
          addVerifiedSubmission(
            Submission.createUnscoredFromCurrentState(
              id = EntityModification.generateRandomId(),
              teamId = team.id,
              value = submissionValue,
              isCorrectAnswer = isCorrectAnswer,
            ),
            resetTimer = question.isInstanceOf[Question.DoubleQ],
            pauseTimer =
              if (question.onlyFirstGainsPoints) isCorrectAnswer == Some(true)
              else shouldPauseTimerBecauseAllTeamsHaveSubmission,
            allowMoreThanOneSubmissionPerTeam = false,
            removeEarlierDifferentSubmissionBySameTeam = !question.onlyFirstGainsPoints,
          )
        } else { // Not multiple choice
          addVerifiedSubmission(
            Submission.createUnscoredFromCurrentState(
              id = EntityModification.generateRandomId(),
              teamId = team.id,
              value = submissionValue,
              isCorrectAnswer = isCorrectAnswer,
            ),
            pauseTimer =
              if (question.onlyFirstGainsPoints) true else shouldPauseTimerBecauseAllTeamsHaveSubmission,
            allowMoreThanOneSubmissionPerTeam = question.onlyFirstGainsPoints,
            removeEarlierDifferentSubmissionBySameTeam = !question.onlyFirstGainsPoints,
          )
        }
      }
    }

    private def addVerifiedSubmission(
        submission: Submission,
        resetTimer: Boolean = false,
        pauseTimer: Boolean = false,
        allowMoreThanOneSubmissionPerTeam: Boolean,
        removeEarlierDifferentSubmissionBySameTeam: Boolean = false,
    )(implicit quizState: QuizState): Unit = {
      val submissionsToRemove = {
        if (removeEarlierDifferentSubmissionBySameTeam) {
          def differentSubmissionBySameTeam(s: Submission): Boolean = {
            s.teamId == submission.teamId && s.value != submission.value
          }
          quizState.submissions.filter(differentSubmissionBySameTeam)
        } else {
          Seq()
        }
      }

      val newQuizState = {
        val newSubmissions = {
          val filteredOldSubmissions = quizState.submissions.filterNot(submissionsToRemove.toSet)
          val submissionAlreadyExists = filteredOldSubmissions.exists(_.teamId == submission.teamId)

          require(
            !submissionAlreadyExists || allowMoreThanOneSubmissionPerTeam,
            s"Could not add submission $submission because this team already has a submission",
          )
          filteredOldSubmissions :+ submission
        }

        quizState.copy(
          timerState =
            if (resetTimer) TimerState.createStarted()
            else if (pauseTimer)
              TimerState(
                lastSnapshotInstant = clock.nowInstant,
                lastSnapshotElapsedTime = quizState.timerState.elapsedTime(),
                timerRunning = false,
                uniqueIdOfMediaPlaying = quizState.timerState.uniqueIdOfMediaPlaying,
              )
            else quizState.timerState,
          submissions = newSubmissions,
        )
      }

      entityAccess.persistEntityModifications(
        Seq(
          EntityModification.createUpdateAllFields(newQuizState),
          EntityModification.createAddWithId(
            submission.id,
            SubmissionEntity(
              teamId = submission.teamId,
              roundIndex = quizState.roundIndex,
              questionIndex = quizState.questionIndex,
              createTime = clock.nowInstant,
              value = submission.value,
              isCorrectAnswer = submission.isCorrectAnswer,
              points = submission.points,
              scored = submission.scored,
            ),
          ),
        ) ++ submissionsToRemove.map(s => EntityModification.Remove[SubmissionEntity](s.id))
      )
    }
  }

  private def fetchAllTeams(): Seq[Team] = {
    entityAccess
      .newQuerySync[Team]()
      .sort(DbQuery.Sorting.ascBy(ModelFields.Team.index))
      .data()
  }

  private def fetchQuizState(): QuizState = {
    entityAccess
      .newQuerySync[QuizState]()
      .findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId) getOrElse QuizState.nullInstance
  }

  private def updateScore(teamId: Long, scoreDiff: FixedPointNumber): Unit = {
    if (scoreDiff != 0) {
      val team = fetchAllTeams().find(_.id == teamId).get
      val oldScore = team.score
      val newScore = oldScore + scoreDiff
      entityAccess.persistEntityModifications(
        EntityModification.createUpdateAllFields(team.copy(score = newScore))
      )
    }
  }

  private def updateSubmissionInStateAndEntity(
      submissionId: Long
  )(update: SubmissionEntity => SubmissionEntity): Unit = {
    val oldSubmissionEntity = entityAccess.newQuerySync[SubmissionEntity]().findById(submissionId)
    val newSubmissionEntity = update(oldSubmissionEntity)

    val oldQuizState = fetchQuizState()
    val newQuizState = oldQuizState.copy(
      submissions = oldQuizState.submissions.map {
        case s if s.id == submissionId => newSubmissionEntity.toSubmission
        case s                         => s
      }
    )

    entityAccess.persistEntityModifications(
      EntityModification.createUpdateAllFields(newQuizState),
      EntityModification.createUpdateAllFields(newSubmissionEntity),
    )
  }

  private def executeInSingleThreadAndWait[R](func: => R): R = {
    singleThreadedExecutor.submit[R](() => func).get()
  }

  private def sendUsageStatisticsToDeveloper(implicit executor: ExecutionContext): Future[Unit] = Future {
    require(quizConfig.usageStatistics.sendAnonymousUsageDataAtEndOfQuiz)

    val submissions =
      entityAccess
        .newQuerySync[SubmissionEntity]()
        .sort(Sorting.ascBy(ModelFields.SubmissionEntity.createTime))
        .data()
    val teams = fetchAllTeams()

    val timeSinceFirstSubmission =
      for (firstSubmission <- submissions.headOption) yield clock.nowInstant - firstSubmission.createTime
    val maxScore: FixedPointNumber = quizConfig.rounds
      .flatMap(_.questions)
      .map(
        // Heuristic because it doesn't take into account extra points for being first.
        _.getPointsToGain(
          submissionValue = None,
          isCorrect = Some(true),
          previousCorrectSubmissionsExist = false,
        )
      )
      .sum
    val medianScore = median(teams.map(_.score))
    val medianScorePercentage = (100 * medianScore.toDouble / maxScore.toDouble).toInt

    val queryParameters: Map[String, Any] = ListMap(
      "project" -> "quizmaster",
      "version" -> AppVersion.versionString,
      "minutesSinceFirstSubmission" -> timeSinceFirstSubmission.map(_.toMinutes).getOrElse("null"),
      "numberOfPlayers" -> teams.size,
      "numberOfQuestions" -> quizConfig.rounds.flatMap(_.questions).size,
      "numberOfSubmissions" -> submissions.size,
      "medianScore" -> s"${medianScorePercentage}_pct",
      "language" -> quizConfig.languageCode,
      "author" -> (if (quizConfig.usageStatistics.includeAuthor) quizConfig.author.getOrElse("") else "null"),
      "quizTitle" -> (if (quizConfig.usageStatistics.includeQuizTitle) quizConfig.title.getOrElse("")
                      else "null"),
    )
    val stringifiedParams = queryParameters
      .map { case (key, value) =>
        var cleanedValue =
          value.toString
            .replace("&", "")
            .replace("\"", "")
            .replace(" ", "_")
        cleanedValue = URLEncoder.encode(cleanedValue, "UTF-8")

        s"$key=$cleanedValue"
      }
      .mkString("&")

    try {
      val response = makeHttpGet(s"https://stats.totw.nl/recordStats?$stringifiedParams")
      require(response.trim == "OK", s"Expected response to be 'OK' but was $response")
    } catch {
      case e: Throwable => println(s"  Failed to send usage statistics: $e")
    }
  }(executor)

  private def makeHttpGet(url: String): String = {
    var inputStream: InputStream = null
    try {
      val urlConnection = new URL(url).openConnection()
      urlConnection.setRequestProperty("User-Agent", """Quizmaster""")
      inputStream = urlConnection.getInputStream()
      val bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
      bufferedReader.readLine()
    } finally {
      if (inputStream != null) {
        inputStream.close()
      }
    }
  }

  private def median[T: Ordering](seq: Seq[T]): T = {
    seq.sorted.drop(seq.length / 2).head
  }

  private object StateUpsertHelper {

    /** Returns true if something changed. */
    def doQuizStateUpsert(update: QuizState => QuizState): Boolean = {
      val maybeQuizState =
        entityAccess.newQuerySync[QuizState]().findOne(ModelFields.QuizState.id === QuizState.onlyPossibleId)

      maybeQuizState match {
        case None =>
          entityAccess.persistEntityModifications(EntityModification.Add(update(QuizState.nullInstance)))
          true
        case Some(quizState2) =>
          val updatedState = update(quizState2)
          if (quizState2 == updatedState) {
            false
          } else {
            entityAccess.persistEntityModifications(EntityModification.createUpdateAllFields(updatedState))
            true
          }
      }
    }

    def goToPreviousStepUpdate(quizState: QuizState): QuizState = {
      implicit val implicitOldQuizState = quizState

      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the end of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              val newRound = quizConfig.rounds(newRoundIndex)
              recoverSubmissions(
                quizState.copy(
                  roundIndex = newRoundIndex,
                  questionIndex = newRound.questions.size - 1,
                  questionProgressIndex = newRound.questions.lastOption.map(_.maxProgressIndex) getOrElse 0,
                  timerState = TimerState.createStarted(),
                  submissions = Seq(),
                  imageIsEnlarged = false,
                )
              )
            case Some(question) if quizState.questionProgressIndex == 0 =>
              // Go to the end of the previous question
              val newQuestionIndex = quizState.questionIndex - 1
              recoverSubmissions(
                quizState.copy(
                  questionIndex = newQuestionIndex,
                  questionProgressIndex = maybeGet(quizState.round.questions, newQuestionIndex)
                    .map(_.maxProgressIndex) getOrElse 0,
                  timerState = TimerState.createStarted(),
                  submissions = Seq(),
                  imageIsEnlarged = false,
                )
              )
            case Some(question) if quizState.questionProgressIndex > 0 =>
              // Decrement questionProgressIndex
              quizState.copy(
                questionProgressIndex = quizState.questionProgressIndex - 1,
                timerState = TimerState.createStarted(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextStepUpdate(quizState: QuizState): QuizState = {
      implicit val implicitOldQuizState = quizState

      quizState.maybeQuestion match {
        case None =>
          goToNextQuestionUpdate(quizState)
        case Some(question) if quizState.questionProgressIndex < question.maxProgressIndex =>
          // Add and remove points
          if (quizState.questionProgressIndex == question.maxProgressIndex - 1) {
            Future(addOrRemovePoints(quizState))
          }

          // Increment questionProgressIndex
          quizState.copy(
            questionProgressIndex = quizState.questionProgressIndex + 1,
            timerState = TimerState.createStarted(),
            imageIsEnlarged = false,
          )
        case Some(question) if quizState.questionProgressIndex == question.maxProgressIndex =>
          goToNextQuestionUpdate(quizState)
      }
    }

    def goToPreviousQuestionUpdate(quizState: QuizState): QuizState = {
      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the start of the last question of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              val newRound = quizConfig.rounds(newRoundIndex)
              recoverSubmissions(
                quizState.copy(
                  roundIndex = newRoundIndex,
                  questionIndex = newRound.questions.size - 1,
                  questionProgressIndex = 0,
                  timerState = TimerState.createStarted(),
                  submissions = Seq(),
                  imageIsEnlarged = false,
                )
              )
            case Some(question) if quizState.questionProgressIndex == 0 =>
              // Go to the start of the previous question
              val newQuestionIndex = quizState.questionIndex - 1
              recoverSubmissions(
                quizState.copy(
                  questionIndex = newQuestionIndex,
                  questionProgressIndex = 0,
                  timerState = TimerState.createStarted(),
                  submissions = Seq(),
                  imageIsEnlarged = false,
                )
              )
            case Some(question) if quizState.questionProgressIndex > 0 =>
              // Go to the start of the question
              quizState.copy(
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextQuestionUpdate(quizState: QuizState): QuizState = {
      quizState.maybeQuestion match {
        case None =>
          if (quizState.round.questions.isEmpty) {
            // Go to next round
            goToNextRoundUpdate(quizState)
          } else {
            // Go to first question
            recoverSubmissions(
              quizState.copy(
                questionIndex = 0,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            )
          }
        case Some(question) =>
          if (quizState.questionIndex == quizState.round.questions.size - 1) {
            // Go to next round
            goToNextRoundUpdate(quizState)
          } else {
            // Go to next question
            recoverSubmissions(
              quizState.copy(
                questionIndex = quizState.questionIndex + 1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            )
          }
      }
    }

    def goToPreviousRoundUpdate(quizState: QuizState): QuizState = {
      quizState.roundIndex match {
        case -1 => quizState // Do nothing
        case _ =>
          quizState.maybeQuestion match {
            case None if quizState.roundIndex == 0 =>
              QuizState.nullInstance
            case None =>
              // Go to the start of the previous round
              val newRoundIndex = Math.min(quizState.roundIndex - 1, quizConfig.rounds.size - 1)
              quizState.copy(
                roundIndex = newRoundIndex,
                questionIndex = -1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
            case Some(question) =>
              // Go to the start of the current round
              quizState.copy(
                questionIndex = -1,
                questionProgressIndex = 0,
                timerState = TimerState.createStarted(),
                submissions = Seq(),
                imageIsEnlarged = false,
              )
          }
      }
    }

    def goToNextRoundUpdate(quizState: QuizState): QuizState = {
      val newQuizState = quizState.copy(
        roundIndex = quizState.roundIndex + 1,
        questionIndex = -1,
        questionProgressIndex = 0,
        timerState = TimerState.createStarted(),
        submissions = Seq(),
        imageIsEnlarged = false,
      )

      if (
        !quizState.quizHasEnded &&
        newQuizState.quizHasEnded &&
        quizConfig.usageStatistics.sendAnonymousUsageDataAtEndOfQuiz
      ) {
        // Return value is unused because we don't wait for the statistics to be sent
        val unusedFuture = sendUsageStatisticsToDeveloper(ExecutionContext.Implicits.global)
      }

      newQuizState
    }

    private def addOrRemovePoints(quizState: QuizState): Unit = executeInSingleThreadAndWait[Unit] {
      val allTeams = fetchAllTeams()
      val unscoredSubmissions = quizState.submissions.filter(!_.scored)

      entityAccess.persistEntityModifications {
        for {
          (teamId, submissionsByTeam) <- unscoredSubmissions.groupBy(_.teamId).toVector
          lastSubmission <- submissionsByTeam.lastOption
          if lastSubmission.points != 0
        } yield {
          val team = allTeams.find(_.id == teamId).get
          val newScore = team.score + lastSubmission.points
          EntityModification.createUpdateAllFields(team.copy(score = newScore))
        }
      }

      for (submission <- unscoredSubmissions) {
        updateSubmissionInStateAndEntity(submission.id)(_.copy(scored = true))
      }
    }

    private def recoverSubmissions(quizState: QuizState): QuizState = {
      require(quizState.submissions.isEmpty)

      val submissionEntities =
        entityAccess
          .newQuerySync[SubmissionEntity]()
          .filter(ModelFields.SubmissionEntity.roundIndex === quizState.roundIndex)
          .filter(ModelFields.SubmissionEntity.questionIndex === quizState.questionIndex)
          .sort(Sorting.ascBy(ModelFields.SubmissionEntity.createTime))
          .data()

      quizState.copy(submissions = submissionEntities.map(_.toSubmission))
    }
  }
}
