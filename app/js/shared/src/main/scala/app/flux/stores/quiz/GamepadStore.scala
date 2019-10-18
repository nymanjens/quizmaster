package app.flux.stores.quiz

import app.flux.stores.quiz.GamepadStore.Arrow
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.GamepadStore.State
import hydro.flux.stores.StateStore
import hydro.jsfacades.Gamepad
import hydro.jsfacades.Gamepad.ButtonIndex
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class GamepadStore extends StateStore[State] {

  private var _state: State = State.nullInstance
  startButtonCheckerLoop()

  override def state: State = _state

  private def setState(newState: State): Unit = {
    if (newState != _state) {
      _state = newState
      Future(invokeStateUpdateListeners())
    }
  }

  private def startButtonCheckerLoop(): Unit = {
    dom.window.setInterval(
      () => {
        setState(getCurrentStateFromControllers())
      },
      /* timeout in millis */ 3
    )
  }

  private def getCurrentStateFromControllers(): State = {
    val gamepads = dom.window.navigator.asInstanceOf[js.Dynamic].getGamepads().asInstanceOf[js.Array[Gamepad]]
    State(gamepads = for (gamepad <- gamepads.toVector) yield {
      val arrowPressed: Option[Arrow] =
        if (isPressed(gamepad, ButtonIndex.up)) Some(Arrow.Up)
        else if (isPressed(gamepad, ButtonIndex.right)) Some(Arrow.Right)
        else if (isPressed(gamepad, ButtonIndex.down)) Some(Arrow.Down)
        else if (isPressed(gamepad, ButtonIndex.left)) Some(Arrow.Left)
        else None

      val otherButtonPressed = Seq(
        ButtonIndex.buttonTop,
        ButtonIndex.buttonRight,
        ButtonIndex.buttonDown,
        ButtonIndex.buttonLeft,
      ).exists(buttonIndex => isPressed(gamepad, buttonIndex))

      GamepadState(
        connected = gamepad.connected,
        arrowPressed = arrowPressed,
        otherButtonPressed = otherButtonPressed,
      )
    })
  }

  private def isPressed(gamepad: Gamepad, buttonIndex: Int): Boolean = {
    if (gamepad.buttons.indices contains buttonIndex) {
      gamepad.buttons(buttonIndex).pressed
    } else {
      false
    }
  }
}

object GamepadStore {
  case class State(gamepads: Seq[GamepadState] = Seq())
  object State {
    def nullInstance = State()
  }

  case class GamepadState(connected: Boolean, arrowPressed: Option[Arrow], otherButtonPressed: Boolean) {
    def anyButtonPressed: Boolean = arrowPressed.isDefined || otherButtonPressed
  }

  sealed trait Arrow
  object Arrow {
    case object Up extends Arrow
    case object Right extends Arrow
    case object Down extends Arrow
    case object Left extends Arrow
  }
}
