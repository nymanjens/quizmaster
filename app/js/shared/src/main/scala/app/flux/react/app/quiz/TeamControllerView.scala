package app.flux.react.app.quiz

import hydro.jsfacades.ReactBeautifulDnd
import hydro.jsfacades.ReactBeautifulDnd.OnDragEndHandler
import app.api.ScalaJsApiClient
import app.common.AnswerBullet
import app.common.LocalStorageClient
import app.flux.action.AppActions
import app.flux.controllers.SoundEffectController
import app.flux.router.AppPages
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.QuizState.Submission.SubmissionValue
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.input.TextInput
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import org.scalajs.dom

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class TeamControllerView(
    implicit pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    clock: Clock,
    quizConfig: QuizConfig,
    soundEffectController: SoundEffectController,
    scalaJsApiClient: ScalaJsApiClient,
    teamEditor: TeamEditor,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizProgressIndicator: QuizProgressIndicator,
    questionComponent: QuestionComponent,
    submissionsSummaryChart: SubmissionsSummaryChart,
    submissionsSummaryTable: SubmissionsSummaryTable,
) {

  // **************** API ****************//
  def apply(teamId: Long, router: RouterContext): VdomElement = {
    <.span(
      ^.className := "team-controller-view",
      teamId match {
        case -1     => CreateTeamForm(router)
        case teamId => Controller(teamId, router)
      }
    )
  }

  def forTeamSelection(router: RouterContext): VdomElement = {
    apply(teamId = -1, router = router)
  }

  private object CreateTeamForm extends HydroReactComponent.Stateless {

    // **************** API ****************//
    def apply(router: RouterContext): VdomElement = {
      component(Props(router))
    }

    // **************** Implementation of HydroReactComponent methods ****************//
    override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

    // **************** Implementation of HydroReactComponent types ****************//
    protected case class Props(router: RouterContext)

    protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

      private val teamNameInputRef = TextInput.ref()

      override def render(props: Props, state: State): VdomElement = logExceptions {
        <.form(
          Bootstrap.FormGroup(
            <.label(i18n("app.enter-team-name")),
            <.div(
              TextInput(
                ref = teamNameInputRef,
                name = "team-name",
                focusOnMount = true,
                defaultValue = LocalStorageClient.getCurrentTeamName() getOrElse "",
              ),
            ),
          ),
          <.div(
            Bootstrap.Button(Variant.primary, Size.sm, tpe = "submit")(
              ^.onClick ==> { (e: ReactEventFromInput) =>
                e.preventDefault()
                val name = teamNameInputRef().valueOrDefault
                if (name.nonEmpty) {
                  Callback.future {
                    getOrCreateTeam(name = name).map { team =>
                      LocalStorageClient.setCurrentTeamName(name)
                      props.router.setPage(AppPages.TeamController(team.id))
                      Callback.empty
                    }
                  }
                } else {
                  Callback.empty
                }
              },
              i18n("app.submit"),
            ),
          ),
        )
      }

      private def getOrCreateTeam(name: String): Future[Team] = async {
        await(teamsAndQuizStateStore.stateFuture).teams.find(_.name == name) match {
          case None       => await(teamsAndQuizStateStore.addTeam(name = name))
          case Some(team) => team
        }
      }
    }
  }

  object Controller extends HydroReactComponent {

    def apply(teamId: Long, router: RouterContext): VdomElement = {
      component(Props(teamId, router))
    }

    // **************** Implementation of HydroReactComponent methods ****************//
    override protected val config =
      ComponentConfig(backendConstructor = new Backend(_), initialState = State())
        .withStateStoresDependencyFromProps { props =>
          StateStoresDependency(
            teamsAndQuizStateStore,
            oldState => {
              makeSoundsAndAlert(
                oldQuizState = oldState.quizState,
                newQuizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
                thisTeamId = props.teamId,
              )
              oldState.copy(
                quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
                teams = teamsAndQuizStateStore.stateOrEmpty.teams,
                maybeTeam = teamsAndQuizStateStore.stateOrEmpty.teams.find(_.id == props.teamId),
              )
            }
          )
        }

    // **************** Private helper methods ****************//
    private def makeSoundsAndAlert(
        oldQuizState: QuizState,
        newQuizState: QuizState,
        thisTeamId: Long,
    ): Unit = {
      val newSubmissionsByThisTeam = {
        val oldSubmissionIds = oldQuizState.submissions.map(_.id).toSet
        newQuizState.submissions
          .filterNot(s => oldSubmissionIds.contains(s.id))
          .filter(_.teamId == thisTeamId)
      }

      // Don't make a sound when the curent submissions are being compared to those of a different
      // question. This includes the case where we're coming from QuizState.nullInstance.
      val questionChanged = oldQuizState.questionIndex != newQuizState.questionIndex ||
        oldQuizState.roundIndex != newQuizState.roundIndex

      // Make sound and alert for new submissions
      for {
        question <- newQuizState.maybeQuestion
        if !questionChanged
        // Only process the most recent submission by this team
        submission <- newSubmissionsByThisTeam.lastOption
      } {
        if (question.isInstanceOf[Question.DoubleQ]) {
          val isCorrect = submission.isCorrectAnswer == Some(true)
          soundEffectController.playRevealingSubmission(correct = isCorrect)
          if (isCorrect) {
            doVibrate()
          }
        } else {
          if (question.onlyFirstGainsPoints && submission.value == SubmissionValue.PressedTheOneButton) {
            // Stopped the timer
            soundEffectController.playNewSubmission()
            doVibrate()
          }
        }
      }
    }

    private def doVibrate(): Unit = {
      dom.window.navigator.asInstanceOf[js.Dynamic].vibrate(400)
    }

    // **************** Implementation of HydroReactComponent types ****************//
    protected case class Props(
        teamId: Long,
        router: RouterContext,
    )
    protected case class State(
        quizState: QuizState = QuizState.nullInstance,
        teams: Seq[Team] = Seq(),
        maybeTeam: Option[Team] = None,
    )

    protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

      private val freeTextAnswerInputRef = TextInput.ref()

      override def render(props: Props, state: State): VdomElement = logExceptions {
        state.maybeTeam match {
          case None       => CreateTeamForm(props.router)
          case Some(team) => controller(team, state.quizState, props.router)
        }
      }

      private def controller(
          implicit team: Team,
          quizState: QuizState,
          router: RouterContext,
      ): VdomElement = {
        <.span(
          <.div(
            ^.className := "team-name",
            team.name,
            " ",
            TeamIcon(team),
          ),
          chooseOtherTeamLink(),
          quizState.maybeQuestion match {
            case Some(question) if showSubmissionForm(question) =>
              <.span(
                <.div(^.className := "question", question.textualQuestion),
                question match {
                  case q: Question.OrderItems                      => orderItemsForm(q)
                  case _ if question.showSingleAnswerButtonToTeams => singleAnswerButton(question)
                  case _ if question.isMultipleChoice              => multipleChoiceAnswerButtons(question)
                  case _                                           => freeTextAnswerForm(question)
                },
              )
            case _ if quizState.quizHasEnded =>
              <.div(
              submissionsSummaryChart(selectedTeamId = Some(team.id)),
              submissionsSummaryTable(selectedTeamId = Some(team.id)),
              )
            case _ =>
              <.span(i18n("app.waiting-for-the-next-question"))
          },
        )
      }

      private def chooseOtherTeamLink()(implicit router: RouterContext): VdomNode = {
        <.div(
          ^.className := "choose-other-team",
          <.a(
            ^.href := "javascript:void",
            ^.onClick --> {
              router.setPage(AppPages.TeamSelection)
              Callback.empty
            },
            "<< ",
            i18n("app.choose-other-team"),
          )
        )
      }

      private def singleAnswerButton(question: Question)(
          implicit team: Team,
          quizState: QuizState,
      ): VdomNode = {
        val canSubmitResponse = quizState.canSubmitResponse(team)

        Bootstrap.Button(
          variant = Variant.primary,
        )(
          ^.className := "the-one-button",
          ^.disabled := !canSubmitResponse,
          ^.onClick --> submitResponse(SubmissionValue.PressedTheOneButton),
          if (question.onlyFirstGainsPoints) {
            i18n("app.stop-the-timer-and-give-the-answer")
          } else {
            i18n("app.indicate-that-you-have-written-down-your-answer")
          },
        )
      }

      private def multipleChoiceAnswerButtons(question: Question)(
          implicit team: Team,
          quizState: QuizState,
      ): VdomNode = {
        val choices = question.maybeTextualChoices.get
        val maybeCurrentSubmissionValue =
          quizState.submissions.filter(_.teamId == team.id).map(_.value).lastOption
        val canSubmitResponse = quizState.canSubmitResponse(team)
        val showSubmissionCorrectness = question.onlyFirstGainsPoints || question.answerIsVisible(
          quizState.questionProgressIndex)

        <.ul(
          ^.className := "multiple-choice-answer-buttons",
          (for ((choice, answerBullet) <- choices zip AnswerBullet.all)
            yield {
              val thisChoiceSubmissionValue = SubmissionValue.MultipleChoiceAnswer(answerBullet.answerIndex)
              val thisChoiceWasChosen = maybeCurrentSubmissionValue == Some(thisChoiceSubmissionValue)
              val thisChoiceIsCorrectAnswer = question.isCorrectAnswer(thisChoiceSubmissionValue) ==
                Some(true)

              <.li(
                ^.key := choice,
                Bootstrap.Button(
                  variant = if (thisChoiceWasChosen) Variant.primary else Variant.default,
                )(
                  ^.disabled := !canSubmitResponse,
                  ^.onClick --> submitResponse(thisChoiceSubmissionValue),
                  ^^.ifThen(thisChoiceWasChosen && showSubmissionCorrectness) {
                    ^.className := (if (thisChoiceIsCorrectAnswer) "correct" else "incorrect")
                  },
                  answerBullet.toVdomNode,
                  <.span(
                    choice,
                  ),
                )
              )
            }).toVdomArray
        )
      }

      private def freeTextAnswerForm(
          question: Question,
          defaultValue: String = "",
          textAlwaysDisabled: Boolean = false,
          clearTextAfterSubmit: Boolean = true,
      )(
          implicit team: Team,
          quizState: QuizState,
      ): VdomNode = {
        val maybeCurrentSubmission = quizState.submissions.filter(_.teamId == team.id).lastOption
        val maybeCurrentSubmissionText =
          maybeCurrentSubmission.map(_.value).flatMap {
            case SubmissionValue.FreeTextAnswer(a) => Some(a)
            case _                                 => None
          }
        val canSubmitResponse = quizState.canSubmitResponse(team)
        val showSubmissionCorrectness = question.onlyFirstGainsPoints || question.answerIsVisible(
          quizState.questionProgressIndex)

        <.form(
          ^.className := "free-text-answer-form",
          Bootstrap.FormGroup(
            <.label(i18n("app.enter-your-answer"), ":"),
            <.div(
              TextInput(
                ref = freeTextAnswerInputRef,
                name = "answer",
                defaultValue = defaultValue,
                focusOnMount = true,
                disabled = !canSubmitResponse || textAlwaysDisabled,
                listener = { newValue =>
                  $.forceUpdate.runNow() // To update OrderItem buttons
                }
              ),
            ),
          ),
          <.div(
            Bootstrap.Button(Variant.primary, Size.sm, tpe = "submit")(
              ^.disabled := !canSubmitResponse,
              ^.onClick ==> { (e: ReactEventFromInput) =>
                e.preventDefault()
                val answer = freeTextAnswerInputRef().valueOrDefault
                val alreadySubmittedThisValue = maybeCurrentSubmission.exists { submission =>
                  submission.value match {
                    case SubmissionValue.FreeTextAnswer(`answer`) => true
                    case _                                        => false
                  }
                }
                if (answer.isEmpty || alreadySubmittedThisValue) {
                  Callback.empty
                } else {
                  if (clearTextAfterSubmit) {
                    freeTextAnswerInputRef().setValue("")
                  }
                  submitResponse(SubmissionValue.FreeTextAnswer(makeWhitespaceVisible(answer)))
                }
              },
              i18n("app.submit"),
            ),
          ),
          <<.ifDefined(maybeCurrentSubmissionText) { currentSubmissionText =>
            <.div(
              ^.className := "you-submitted",
              i18n("app.you-submitted"),
              ": ",
              <.span(
                ^^.ifThen(showSubmissionCorrectness) {
                  ^.className := (if (maybeCurrentSubmission.get.isCorrectAnswer == Some(true)) "correct"
                                  else "incorrect")
                },
                currentSubmissionText,
              )
            )
          },
        )
      }

      private def orderItemsForm(question: Question.OrderItems)(
          implicit team: Team,
          quizState: QuizState,
      ): VdomNode = {
        val canSubmitResponse = quizState.canSubmitResponse(team)
        val maybeCurrentSubmission = quizState.submissions.filter(_.teamId == team.id).lastOption
        val maybeCurrentSubmissionText =
          maybeCurrentSubmission.map(_.value).flatMap {
            case SubmissionValue.FreeTextAnswer(a) => Some(a)
            case _                                 => None
          }
        val items =
          maybeOrderItemsFromTextInput(question) orElse
            maybeCurrentSubmissionText.map(_.map(question.itemFromCharacterCode)) getOrElse
            question.itemsInAlphabeticalOrder

        <.div(
          freeTextAnswerForm(
            question,
            defaultValue = items.map(question.toCharacterCode).mkString,
            textAlwaysDisabled = true,
            clearTextAfterSubmit = false,
          ),
          if (canSubmitResponse) {
            ReactBeautifulDnd.DragDropContext(onDragEndHandler = onDragEndHandler(items, question))(
              ReactBeautifulDnd.Droppable(droppableId = "droppable") {
                (provided, snapshot) =>
                  <.ul(
                    ^.className := "order-items-answer-buttons",
                    rawTagMod("ref", provided.innerRef),
                    (for ((item, index) <- items.zipWithIndex)
                      yield {
                        ReactBeautifulDnd.Draggable(
                          key = s"draggable-${item.item}",
                          draggableId = item.item,
                          index = index,
                        ) { (provided, snapshot) =>
                          <.li(toTagMods(provided.draggableProps) ++ toTagMods(provided.dragHandleProps): _*)(
                            ^.key := s"item-${item.item}",
                            rawTagMod("ref", provided.innerRef),
                            <.div(
                              ^.className := "draggable-button",
                              Bootstrap.FontAwesomeIcon("arrows-v"),
                              " ",
                              s"${question.toCharacterCode(item)}/ ${item.item}",
                            )
                          )
                        }
                      }).toVdomArray
                  )
              }
            )
          } else {
            <<.ifDefined(maybeCurrentSubmissionText) {
              submissionText =>
                val submittedItems = submissionText.map(question.itemFromCharacterCode)
                <.ul(
                  ^.className := "order-items-answer-buttons",
                  (for (item <- submittedItems)
                    yield
                      <.li(
                        ^.key := s"item-${item.item}",
                        <.div(
                          ^.className := "draggable-button disabled",
                          Bootstrap.FontAwesomeIcon("arrows-v"),
                          " ",
                          s"${question.toCharacterCode(item)}/ ${item.item}",
                        )
                      )).toVdomArray
                )
            }
          }
        )
      }
      private def onDragEndHandler(
          items: Seq[Question.OrderItems.Item],
          question: Question.OrderItems,
      ): OnDragEndHandler = { (sourceIndex, maybeDestinationIndex) =>
        if (maybeDestinationIndex.isDefined) {
          val destinationIndex = maybeDestinationIndex.get

          val newItems = {
            val resultBuilder = items.toBuffer
            resultBuilder.remove(sourceIndex)
            resultBuilder.insert(destinationIndex, items(sourceIndex))
            resultBuilder.toVector
          }

          val newAnswerString = newItems.map(question.toCharacterCode).mkString
          freeTextAnswerInputRef.apply().setValue(newAnswerString)
        }
      }

      private def maybeOrderItemsFromTextInput(
          question: Question.OrderItems): Option[Seq[Question.OrderItems.Item]] = {
        val answerString = freeTextAnswerInputRef.apply().valueOrDefault
        if (answerString != null && question.isValidAnswerString(answerString)) {
          Some(answerString.map(question.itemFromCharacterCode))
        } else {
          None
        }
      }

      private def submitResponse(submissionValue: SubmissionValue)(implicit team: Team): Callback = {
        Callback.future {
          dispatcher
            .dispatch(AppActions.AddSubmission(teamId = team.id, submissionValue = submissionValue))
            .map(_ => Callback.empty)
        }
      }

      private def showSubmissionForm(question: Question)(implicit quizState: QuizState): Boolean = {
        // Show the form if the question in the right state. If this is a question where teams submitted anything,
        // it makes sense to keep showing their submission (even if this particular team didn't submit anything).
        question.submissionAreOpen(quizState.questionProgressIndex) || quizState.submissions.nonEmpty
      }

      private def makeWhitespaceVisible(s: String): String = {
        if (s.trim.isEmpty) "<whitespace>" else s
      }

      private def toTagMods(props: js.Dictionary[js.Object]): Seq[TagMod] = {
        val scalaProps: mutable.Map[String, js.Object] = props
        for ((name, value) <- scalaProps.toVector) yield rawTagMod(name, value)
      }

      private def rawTagMod(name: String, value: js.Object): TagMod = TagMod.fn(_.addAttr(name, value))
    }
  }
}
