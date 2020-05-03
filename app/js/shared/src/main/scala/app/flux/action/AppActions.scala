package app.flux.action

import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.flux.action.Action

object AppActions {

  // **************** Quiz-related actions **************** //
  case class AddSubmission(teamId: Long, submissionValue: SubmissionValue) extends Action
}
