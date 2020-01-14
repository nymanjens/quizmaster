package app.flux.react.app.quiz

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.Team
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Table
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import org.scalajs.dom

import scala.collection.immutable.Seq

final class ImportBackupPanel(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()
  protected case class State(
      importString: String = "",
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val _: State = state
      <.span(
        Bootstrap.Row(
          HalfPanel(title = <.span("Import backup (see console output)")) {
            <.form(
              <.input(
                ^.tpe := "text",
                ^.name := s"import-string",
                ^.value := state.importString,
                ^.autoComplete := "off",
                ^.onChange ==> { (e: ReactEventFromInput) =>
                  logExceptions {
                    val newString = e.target.value
                    $.modState(_.copy(importString = newString))
                  }
                }
              ),
              " ",
              importBackupButton()
            )
          }
        )
      )
    }

    private def importBackupButton()(implicit state: State): VdomNode = {
      Bootstrap.Button(Variant.info, tpe = "submit")(
        Bootstrap.Glyphicon("import"),
        ^.onClick ==> { (e: ReactEventFromInput) =>
          e.preventDefault()
          Callback.future {
            teamsAndQuizStateStore
              .replaceAllEntitiesByImportString(state.importString)
              .map(_ => Callback.empty)
          }
        }
      )
    }
  }
}
