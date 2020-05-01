package app.flux.stores.quiz

import japgolly.scalajs.react.vdom.html_<^._
import app.flux.stores.quiz.GamepadStore.Arrow
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.GamepadStore.State
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.stores.StateStore
import hydro.jsfacades.Gamepad
import hydro.jsfacades.Gamepad.ButtonIndex
import japgolly.scalajs.react.vdom.VdomElement
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class GamepadStore extends StateStore[State] {

  private var _state: State = State.nullInstance
  startButtonCheckerLoop()

  override def state: State = _state

  def rumble(gamepadIndex: Int): Unit = {
    val gamepads = getGamepads()
    if (gamepads.indices contains gamepadIndex) {
      val gamepad = gamepads(gamepadIndex)
      if (gamepad != null && gamepad.connected) {
        val vibrationActuator = gamepad.vibrationActuator
        if (vibrationActuator != null && !js.isUndefined(vibrationActuator)) {
          vibrationActuator.playEffect(
            "dual-rumble",
            js.Dictionary(
              "startDelay" -> 0,
              "duration" -> 400,
              "weakMagnitude" -> 1.0,
              "strongMagnitude" -> 1.0,
            ))
        }
      }
    }
  }

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

  private def getGamepads(): Seq[Gamepad] = {
    dom.window.navigator.asInstanceOf[js.Dynamic].getGamepads().asInstanceOf[js.Array[Gamepad]].toVector
  }

  private def getCurrentStateFromControllers(): State = {
    State(gamepads = for (gamepad <- getGamepads()) yield {
      if (gamepad != null && gamepad.connected) {
        val arrowPressed: Option[Arrow] =
          if (isPressed(gamepad, ButtonIndex.up)) Some(Arrow.Up)
          else if (isPressed(gamepad, ButtonIndex.right)) Some(Arrow.Right)
          else if (isPressed(gamepad, ButtonIndex.down)) Some(Arrow.Down)
          else if (isPressed(gamepad, ButtonIndex.left)) Some(Arrow.Left)
          // Some gamepads that don't have an analog stick have the arrow keys mapped to analog with no arrow buttons.
          // The following cases handle this.
          else if (gamepad.axes(0) == -1) Some(Arrow.Left)
          else if (gamepad.axes(0) == 1) Some(Arrow.Right)
          else if (gamepad.axes(1) == -1) Some(Arrow.Up)
          else if (gamepad.axes(1) == 1) Some(Arrow.Down)
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
      } else {
        GamepadState.nullInstance
      }
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

  case class GamepadState(
      connected: Boolean = false,
      arrowPressed: Option[Arrow] = None,
      otherButtonPressed: Boolean = false,
  ) {
    def anyButtonPressed: Boolean = arrowPressed.isDefined || otherButtonPressed
  }
  object GamepadState {
    val nullInstance = GamepadState()
  }

  sealed abstract class Arrow {
    lazy val answerIndex: Int = Arrow.all.indexOf(this)
  }
  object Arrow {
    def all: Seq[Arrow] = Seq(Up, Right, Down, Left)

    case object Up extends Arrow
    case object Right extends Arrow
    case object Down extends Arrow
    case object Left extends Arrow
  }
}
