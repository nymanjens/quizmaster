package app.common

import hydro.common.GuavaReplacement.Splitter
import hydro.common.ScalaUtils.ifThenOption

object FilePathUtils {

  def maybeGetExtensionIncludingDot(fpath: String): Option[String] = {
    val parts = Splitter.on('.').trimResults().omitEmptyStrings().split(fpath)
    ifThenOption(parts.size >= 2 && parts.last.size >= 2 && parts.last.size <= 5) {
      "." + parts.last
    }
  }

  def maybeStripExtension(fpath: String): String = {
    maybeGetExtensionIncludingDot(fpath) match {
      case Some(extension) =>
        require(fpath.endsWith(extension))
        fpath.substring(0, fpath.size - extension.size)
      case None => fpath
    }
  }
}
