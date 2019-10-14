package app.common

import hydro.common.GuavaReplacement.Splitter

object RelativePaths {

  def joinPaths(s1: String, s2: String): String = {
    if (s1.endsWith("/")) {
      s1 + s2
    } else {
      s1 + "/" + s2
    }
  }

  def getFilename(relativePath: String): String = Splitter.on('/').split(relativePath).last

  def getFolderPath(relativePath: String): String =
    relativePath.stripSuffix(getFilename(relativePath)).stripSuffix("/")
}
