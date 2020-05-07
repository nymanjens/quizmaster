package app.common

import org.scalajs.dom

object LocalStorageClient {

  def getCurrentTeamName(): Option[String] = {
    Option(dom.window.localStorage.getItem("current-team-name"))
  }
  def setCurrentTeamName(name: String): Unit = {
    dom.window.localStorage.setItem("current-team-name", name)
  }

  def getMasterSecret(): Option[String] = {
    Option(dom.window.localStorage.getItem("master-secret"))
  }

  def setMasterSecret(secret: String): Unit = {
    dom.window.localStorage.setItem("master-secret", secret)
  }
  def removeMasterSecret(): Unit = {
    dom.window.localStorage.removeItem("master-secret")
  }
}
