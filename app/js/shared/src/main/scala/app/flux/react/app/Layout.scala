package app.flux.react.app

import app.flux.react.app.quiz.TeamsList
import app.flux.router.AppPages
import hydro.flux.react.uielements.SbadminLayout
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class Layout(
    implicit sbadminLayout: SbadminLayout,
    teamsList: TeamsList,
) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      implicit val router = props.router
      sbadminLayout(
        title = "Quizmaster",
        leftMenu = <.span(),
        pageContent = <.div(
          ^.id := "content-wrapper",
          <.div(
            ^.id := "left-content-wrapper",
            teamsList(showScoreEditButtons = router.currentPage == AppPages.Master),
          ),
          <.div(
            ^.id := "right-content-wrapper",
            children
          ),
        ),
      )
    }
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
