package app.common

import org.scalajs.dom

object JsQuizAssets {

  def encodeSource(assetSource: String): String = {
    encodeBase64(assetSource)
  }

  private def encodeBase64(string: String): String = {
    dom.window.btoa(string)
  }
}
