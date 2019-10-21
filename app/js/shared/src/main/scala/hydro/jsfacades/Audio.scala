package hydro.jsfacades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
final class Audio(filename: String) extends js.Object {

  def play(): js.Promise[Unit] = js.native
  def addEventListener(eventName: String, listener: js.Function0[Unit]): Unit = js.native
}
