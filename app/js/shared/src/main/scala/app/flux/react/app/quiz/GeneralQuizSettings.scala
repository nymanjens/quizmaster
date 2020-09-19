package app.flux.react.app.quiz

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import app.api.ScalaJsApi.GetInitialDataResponse
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import hydro.common.I18n
import hydro.common.JsLoggingUtils
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.HalfPanel
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.concurrent.Future

final class GeneralQuizSettings(implicit
    i18n: I18n,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        quizState = teamsAndQuizStateStore.stateOrEmpty.quizState
      ),
    )

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props()
  protected case class State(
      quizState: QuizState = QuizState.nullInstance
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      val generalQuizSettings = state.quizState.generalQuizSettings
      <.span(
        ^.className := "general-quiz-settings",
        Bootstrap.Row(
          HalfPanel(
            title = <.span("General quiz settings")
          ) {
            <.span(
              <.div(
                ^.className := "single-quiz-setting",
                "Show answers: ",
                toggleButttonGroup(
                  valuesToLabelMap = Map(false -> "No", true -> "Yes"),
                  selectedValue = generalQuizSettings.showAnswers,
                  updateValueFunction = teamsAndQuizStateStore.setShowAnswers,
                ),
              ),
              <.div(
                ^.className := "single-quiz-setting",
                "Answer bullet type: ",
                toggleButttonGroup[AnswerBulletType](
                  valuesToLabelMap =
                    Map(AnswerBulletType.Arrows -> "Arrows", AnswerBulletType.Characters -> "Characters"),
                  selectedValue = generalQuizSettings.answerBulletType,
                  updateValueFunction = teamsAndQuizStateStore.setAnswerBulletType,
                ),
              ),
            )
          }
        ),
      )
    }

    private def toggleButttonGroup[T](
        valuesToLabelMap: Map[T, String],
        selectedValue: T,
        updateValueFunction: T => Future[Unit],
    ): VdomElement = {
      Bootstrap.ButtonGroup(
        (
          for ((value, label) <- valuesToLabelMap)
            yield Bootstrap.Button(
              variant = if (value == selectedValue) Variant.primary else Variant.default
            )(
              ^.onClick --> Callback.future(updateValueFunction(value).map(_ => Callback.empty)),
              ^.key := value.toString,
              label,
            )
        ).toVdomArray
      )
    }
  }
}
