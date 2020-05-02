package app.flux.controllers

import scala.concurrent.duration._
import app.flux.router.AppPages
import app.models.access.ModelFields
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Action
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.router.Page
import hydro.jsfacades.Audio
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.scalajs.js

final class SoundEffectController(
    implicit dispatcher: Dispatcher,
    entityAccess: JsEntityAccess,
) {
  private var currentPage: Page = _
  private val soundsPlaying: mutable.Set[SoundEffect] = mutable.Set()

  dispatcher.registerPartialSync(dispatcherListener)
  entityAccess.registerListener(JsEntityAccessListener)

  // **************** Public API ****************//
  def playNewSubmission(): Unit = playSoundEffect(SoundEffect.NewSubmission)
  def playRevealingSubmission(correct: Boolean): Unit = {
    if (correct) {
      playSoundEffect(SoundEffect.CorrectSubmission)
    } else {
      playSoundEffect(SoundEffect.IncorrectSubmission)
    }
  }
  def playTimerRunsOut(): Unit = playSoundEffect(SoundEffect.TimerRunsOut)

  // **************** Private helper methods ****************//
  private def dispatcherListener: PartialFunction[Action, Unit] = {
    case StandardActions.SetPageLoadingState( /* isLoading = */ _, currentPage) =>
      this.currentPage = currentPage
  }

  private def canPlaySoundEffectsOnThisPage: Boolean = currentPage == AppPages.Quiz

  private def playSoundEffect(
      soundEffect: SoundEffect,
      minTimeBetweenPlays: Option[FiniteDuration] = None,
  ): Unit =
    logExceptions {
      if (canPlaySoundEffectsOnThisPage) {
        if (minTimeBetweenPlays.isDefined && (soundsPlaying contains soundEffect)) {
          // Skip
        } else {
          soundsPlaying.add(soundEffect)
          val audio = new Audio(soundEffect.filepath)
          audio.addEventListener(
            "ended",
            () => {
              val timeoutTime = minTimeBetweenPlays getOrElse (0.seconds)
              js.timers.setTimeout(timeoutTime)(logExceptions {
                soundsPlaying.remove(soundEffect)
              })
            }
          )

          println(s"  Playing ${soundEffect.filepath}..")
          audio.play()
        }
      }
    }

  private object JsEntityAccessListener extends JsEntityAccess.Listener {
    override def modificationsAddedOrPendingStateChanged(modifications: Seq[EntityModification]): Unit = {
      modifications.collect {
        case EntityModification.Update(team: Team) =>
          if (Some(team.lastUpdateTime.mostRecentInstant) ==
                team.lastUpdateTime.timePerField.get(ModelFields.Team.score)) {
            playSoundEffect(SoundEffect.ScoreIncreased)
          }
      }
    }
  }

  private sealed abstract class SoundEffect(val filepath: String)
  private object SoundEffect {
    case object NewSubmission extends SoundEffect("/assets/soundeffects/new_submission.mp3")
    case object CorrectSubmission extends SoundEffect("/assets/soundeffects/correct_submission.mp3")
    case object IncorrectSubmission extends SoundEffect("/assets/soundeffects/incorrect_submission.mp3")
    case object ScoreIncreased extends SoundEffect("/assets/soundeffects/score_increased.mp3")
    case object TimerRunsOut extends SoundEffect("/assets/soundeffects/timer_runs_out.mp3")
  }
}
