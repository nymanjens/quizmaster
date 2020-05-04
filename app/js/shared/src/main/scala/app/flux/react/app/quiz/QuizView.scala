package app.flux.react.app.quiz

import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class QuizView(
    implicit pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamEditor: TeamEditor,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizProgressIndicator: QuizProgressIndicator,
    questionComponent: QuestionComponent,
    soundEffectController: SoundEffectController,
    teamInputStore: TeamInputStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      oldState => {
        makeSoundsAndAlert(
          oldQuizState = oldState.quizState,
          newQuizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
          oldTeams = oldState.teams,
          newTeams = teamsAndQuizStateStore.stateOrEmpty.teams,
        )
        oldState.copy(
          teams = teamsAndQuizStateStore.stateOrEmpty.teams,
          quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
        )
      }
    )

  // **************** Private helper methods ****************//
  private def makeSoundsAndAlert(
      oldQuizState: QuizState,
      newQuizState: QuizState,
      newTeams: Seq[Team],
      oldTeams: Seq[Team],
  ): Unit = {
    // Make sound and alert for new submissions
    for (question <- newQuizState.maybeQuestion) {
      val newSubmissions = {
        val oldSubmissionIds = oldQuizState.submissions.map(_.id).toSet
        newQuizState.submissions.filterNot(s => oldSubmissionIds.contains(s.id))
      }
      if (oldQuizState != QuizState.nullInstance && newSubmissions.nonEmpty) {
        if (question.onlyFirstGainsPoints && newSubmissions.exists(_.value.isScorable)) {
          // An answer was given that will be immediately visible, so the sound can indicate its correctness
          val atLeastOneSubmissionIsCorrect = newSubmissions.exists(_.isCorrectAnswer)
          soundEffectController.playRevealingSubmission(correct = atLeastOneSubmissionIsCorrect)
        } else {
          soundEffectController.playNewSubmission()
        }

        if (question.isInstanceOf[Question.Double]) {
          for (submission <- newSubmissions) {
            if (submission.isCorrectAnswer) {
              teamInputStore.alertTeam(submission.teamId)
            }
          }
        }
      }
    }

    // Make sound if score changed
    val scoreIncreased = {
      val oldTeamsMap = oldTeams.map(t => (t.id -> t)).toMap
      newTeams.exists { newTeam =>
        val maybeOldTeam = oldTeamsMap.get(newTeam.id)
        maybeOldTeam.isDefined && newTeam.score > maybeOldTeam.get.score
      }
    }
    if (scoreIncreased) {
      soundEffectController.playScoreIncreased()
    }
  }

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
      quizState: QuizState = QuizState.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      val quizState = state.quizState

      <.span(
        ^.className := "quiz-view",
        quizProgressIndicator(state.quizState, showMasterData = false),
        quizState.maybeQuestion match {
          case None =>
            RoundComponent(quizState.round)
          case Some(question) =>
            questionComponent(
              question = question,
              round = state.quizState.round,
              questionProgressIndex = quizState.questionProgressIndex,
              showMasterData = false,
            )
        },
      )
    }
  }
}
