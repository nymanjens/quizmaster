package app.flux.react.app.quiz

import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class QuizSettingsPanels(
    implicit teamEditor: TeamEditor,
    importBackupPanel: ImportBackupPanel,
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      <.div(
        importBackupPanel(),
        teamEditor(),
      )
    }
  }
}
