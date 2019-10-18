package hydro.jsfacades

import hydro.jsfacades.Gamepad.GamepadButton

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

@js.native
trait Gamepad extends js.Object {

  def connected: Boolean = js.native
  def buttons: js.Array[GamepadButton] = js.native
}

object Gamepad {
  @js.native
  trait GamepadButton extends js.Object {
    def pressed: Boolean = js.native
  }

  // Standard button mappings: https://w3c.github.io/gamepad/#remapping
  object ButtonIndex {
    // Arrows in the left cluster
    val up: Int = 12
    val right: Int = 15
    val down: Int = 13
    val left: Int = 14

    // Buttons in the right cluster
    val buttonTop: Int = 3
    val buttonRight: Int = 1
    val buttonDown: Int = 0
    val buttonLeft: Int = 2
  }
}
