package app.common

import app.api.ScalaJsApi.GetInitialDataResponse
import app.flux.router.AppPages
import hydro.flux.router.RouterContext
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

object MasterSecretUtils {

  def requireMasterSecretOrRedirect(
      returnValueIfMaster: => VdomElement,
      router: RouterContext,
  )(implicit getInitialDataResponse: GetInitialDataResponse): VdomElement = {
    if (LocalStorageClient.getMasterSecret() == Some(getInitialDataResponse.masterSecret)) {
      returnValueIfMaster
    } else {
      println(
        s"  Redirecting to TeamController because stored master secret " +
          s"(${LocalStorageClient.getMasterSecret()}) is different from the required one")
      router.setPage(AppPages.TeamController)
      <.span()
    }
  }
}
