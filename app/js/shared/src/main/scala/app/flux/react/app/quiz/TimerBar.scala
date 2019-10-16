package app.flux.react.app.quiz

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import java.time.Duration
import java.time.Instant

import hydro.common.time.JavaTimeImplicits._
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.concurrent.Future

final class TimerBar(
    implicit clock: Clock,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(maxTime: Duration, onFinished: () => Unit): VdomElement = {
    component(Props(maxTime = maxTime, onFinished = onFinished))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(maxTime: Duration, onFinished: () => Unit)
  protected case class State(elapsedTime: Duration = Duration.ZERO, paused: Boolean = false)

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with WillUnmount {

    private var intervalHandle: Option[Int] = None
    private var lastIntervalCycleTime: Instant = Instant.EPOCH

    override def render(props: Props, state: State): VdomElement = logExceptions {
      val timeLeft = props.maxTime - state.elapsedTime
      val timeLeftFraction = timeLeft / props.maxTime
      Bootstrap.ProgressBar(
        fraction = timeLeftFraction,
        variant = if (timeLeftFraction < 0.1) Variant.danger else Variant.info)
    }

    override def didMount(props: Props, state: State): Callback = {
      startOrResumeCountDown()
      Callback.empty
    }
    override def willUnmount(props: Props, state: State): Callback = {
      clearInterval()
      Callback.empty
    }

    private def startOrResumeCountDown(): Unit = {
      clearInterval()
      lastIntervalCycleTime = clock.nowInstant
      intervalHandle = Some(
        dom.window.setInterval(
          () => {
            val now = clock.nowInstant
            val diffWithLastTime = now - lastIntervalCycleTime

            val props = $.props.runNow()

            $.modState(state => {
              val newElapsedTime = Seq(state.elapsedTime + diffWithLastTime, props.maxTime).min
              if (newElapsedTime == props.maxTime) {
                Future {
                  clearInterval()
                  props.onFinished()
                }
              }
              state.copy(elapsedTime = newElapsedTime)
            }).runNow()

            lastIntervalCycleTime = now
          },
          /* timeout in millis */ 10
        ))
    }
    private def clearInterval(): Unit = {
      for (handle <- intervalHandle) dom.window.clearInterval(handle)
    }

    private def bindShortcuts(): Unit = {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      bind("space", () => togglePaused())
    }

    private def togglePaused(): Unit = {
      $.modState(state => {
        val newPaused = !state.paused
        if (newPaused) {
          clearInterval()
        } else {
          startOrResumeCountDown()
        }
        state.copy(paused = newPaused)
      }).runNow()
    }
  }
}
