package app.flux

import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.logFailure
import hydro.common.MobileUtils
import hydro.jsfacades.Audio
import org.scalajs.dom
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.mutable
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

object ClientApp {

  def main(): Unit = async {
    console.log("  Application starting")

    // create stylesheet
    //GlobalStyles.addToDocument()

    setUpNoSleep()

    val commonTimeModule = new hydro.common.time.Module
    implicit val clock = commonTimeModule.clock

    val apiModule = new app.api.Module
    implicit val scalaJsApiClient = apiModule.scalaJsApiClient
    implicit val initialDataResponse = await(logFailure(scalaJsApiClient.getInitialData()))

    logExceptions {
      implicit val globalModule = new ClientAppModule()

      preloadResources(initialDataResponse.quizConfig)

      // tell React to render the router in the document body
      globalModule.router().renderIntoDOM(dom.document.getElementById("root"))
    }
  }

  // Snippet copied from https://stackoverflow.com/a/46363553
  private def setUpNoSleep(): Unit = logExceptions {
    if (MobileUtils.isMobileOrTablet) {
      def base64(mimeType: String, base64: String): String = {
        "data:" + mimeType + ";base64," + base64
      }

      var video = dom.document.createElement("video").asInstanceOf[dom.raw.HTMLVideoElement]
      video.setAttribute("loop", "")

      def addSourceToVideo(element: dom.raw.Element, tpe: String, dataURI: String) = {
        val source = dom.document.createElement("source").asInstanceOf[js.Dynamic]
        source.src = dataURI
        source.tpe = "video/" + tpe
        element.appendChild(source.asInstanceOf[dom.raw.Node])
      }

      addSourceToVideo(
        video,
        "webm",
        base64(
          "video/webm",
          "GkXfo0AgQoaBAUL3gQFC8oEEQvOBCEKCQAR3ZWJtQoeBAkKFgQIYU4BnQI0VSalmQCgq17FAAw9CQE2AQAZ3aGFtbXlXQUAGd" +
            "2hhbW15RIlACECPQAAAAAAAFlSua0AxrkAu14EBY8WBAZyBACK1nEADdW5khkAFVl9WUDglhohAA1ZQOIOBAeBABrCBCLqB" +
            "CB9DtnVAIueBAKNAHIEAAIAwAQCdASoIAAgAAUAmJaQAA3AA/vz0AAA="
        )
      )
      addSourceToVideo(
        video,
        "mp4",
        base64(
          "video/mp4",
          "AAAAHGZ0eXBpc29tAAACAGlzb21pc28ybXA0MQAAAAhmcmVlAAAAG21kYXQAAAGzABAHAAABthADAowdbb9/AAAC6W1vb3YAA" +
            "ABsbXZoZAAAAAB8JbCAfCWwgAAAA+gAAAAAAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAA" +
            "AAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAIVdHJhawAAAFx0a2hkAAAAD3wlsIB8JbCAAAAAAQAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAIAAAACAAAAAABsW1kaW" +
            "EAAAAgbWRoZAAAAAB8JbCAfCWwgAAAA+gAAAAAVcQAAAAAAC1oZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAVmlkZW9IY" +
            "W5kbGVyAAAAAVxtaW5mAAAAFHZtaGQAAAABAAAAAAAAAAAAAAAkZGluZgAAABxkcmVmAAAAAAAAAAEAAAAMdXJsIAAAAAEA" +
            "AAEcc3RibAAAALhzdHNkAAAAAAAAAAEAAACobXA0dgAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAIAAgASAAAAEgAAAAAAAA" +
            "AAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABj//wAAAFJlc2RzAAAAAANEAAEABDwgEQAAAAADDUAAAAAABS" +
            "0AAAGwAQAAAbWJEwAAAQAAAAEgAMSNiB9FAEQBFGMAAAGyTGF2YzUyLjg3LjQGAQIAAAAYc3R0cwAAAAAAAAABAAAAAQAAA" +
            "AAAAAAcc3RzYwAAAAAAAAABAAAAAQAAAAEAAAABAAAAFHN0c3oAAAAAAAAAEwAAAAEAAAAUc3RjbwAAAAAAAAABAAAALAAA" +
            "AGB1ZHRhAAAAWG1ldGEAAAAAAAAAIWhkbHIAAAAAAAAAAG1kaXJhcHBsAAAAAAAAAAAAAAAAK2lsc3QAAAAjqXRvbwAAABt" +
            "kYXRhAAAAAQAAAABMYXZmNTIuNzguMw=="
        )
      )

      video.play()
    }
  }

  private val preloadedImages: mutable.Buffer[HtmlImage] = mutable.Buffer()
  private val preloadedAudios: mutable.Buffer[Audio] = mutable.Buffer()

  private def preloadResources(quizConfig: QuizConfig): Unit = {

    for {
      round <- quizConfig.rounds
      question <- round.questions
    } {
      question match {
        case single: Question.Single =>
          for (image <- Seq() ++ single.image ++ single.answerImage) {
            val htmlImage = new HtmlImage()
            htmlImage.asInstanceOf[js.Dynamic].src = s"/quizimages/${image.src}"
            preloadedImages.append(htmlImage)
          }

          for (audioSrc <- single.audioSrc) {
            val audio = new Audio(s"/quizaudio/$audioSrc")
            preloadedAudios.append(audio)
          }
        case _ =>
      }
    }
  }

  @js.native
  @JSGlobal(name = "Image")
  class HtmlImage extends js.Object {
    // var src: js.Any = js.native // Doesn't compile for unknown reason
  }
}
