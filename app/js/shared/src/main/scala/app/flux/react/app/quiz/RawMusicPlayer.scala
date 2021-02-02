package app.flux.react.app.quiz

import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import org.scalajs.dom.raw.HTMLAudioElement

private[quiz] object RawMusicPlayer extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      src: String,
      playing: Boolean,
      key: String = "raw-music-player",
      showControls: Boolean = false,
  ): VdomElement = {
    val props = Props(
      src = src,
      playing = playing,
      showControls = showControls,
    )
    component.withKey(key).apply(props)
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props private[RawMusicPlayer] (
      src: String,
      playing: Boolean,
      showControls: Boolean,
  )

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with DidUpdate {
    private val audioRef = Ref[html.Audio]

    override def didMount(props: Props, state: Unit): Callback = LogExceptionsCallback {
      if (props.playing) {
        htmlAudioElement match {
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
        htmlAudioElement match {
          case Some(e) if currentProps.playing =>
            if (e.ended) { /* Do nothing because the music is already done */ }
            else { e.play() }
          case Some(e) if !currentProps.playing => e.pause()
          case None                             =>
        }
      }
    }

    override def render(props: Props, state: State) = logExceptions {
      <.audio(
        ^.controls := props.showControls,
        ^.src := props.src,
        ^.preload := "auto",
      ).withRef(audioRef)
    }

    def htmlAudioElement: Option[HTMLAudioElement] = audioRef.get.asCallback.runNow()
  }
}
