package app.flux.react.app.quiz

import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLVideoElement

private[quiz] object RawVideoPlayer extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      src: String,
      playing: Boolean,
      key: String,
  ): VdomElement = {
    val props = Props(
      src = src,
      playing = playing,
    )
    component.withKey(key).apply(props)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props private[RawVideoPlayer] (
      src: String,
      playing: Boolean,
  )

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with DidUpdate {
    private val videoRef = Ref[html.Video]

    override def didMount(props: Props, state: Unit): Callback = LogExceptionsCallback {
      if (props.playing) {
        htmlVideoElement match {
          case Some(e) => e.play()
          case None    =>
        }
      }
    }

    override def didUpdate(
        prevProps: Props,
        currentProps: Props,
        prevState: Unit,
        currentState: Unit,
    ): Callback = LogExceptionsCallback {
      if (prevProps != currentProps) {
        htmlVideoElement match {
          case Some(e) if currentProps.playing  => e.play()
          case Some(e) if !currentProps.playing => e.pause()
          case None                             =>
        }
      }
    }

    override def render(props: Props, state: State) = logExceptions {
      <.video(
        ^.controls := true,
        ^.width := "320",
        ^.preload := "auto",
        <.source(^.src := props.src),
      ).withRef(videoRef)
    }

    def htmlVideoElement: Option[HTMLVideoElement] = videoRef.get.asCallback.runNow()
  }
}
