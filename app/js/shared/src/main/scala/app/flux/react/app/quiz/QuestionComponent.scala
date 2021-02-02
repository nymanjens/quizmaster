package app.flux.react.app.quiz

import app.common.AnswerBullet
import app.common.JsQuizAssets
import app.flux.controllers.SoundEffectController
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.Team
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.common.I18n
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.BootstrapTags
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.VdomNode

final class QuestionComponent(implicit
    pageHeader: PageHeader,
    i18n: I18n,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    obfuscatedAnswer: ObfuscatedAnswer,
    clock: Clock,
    soundEffectController: SoundEffectController,
    teamInputStore: TeamInputStore,
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      showMasterData: Boolean,
      quizState: QuizState,
      teams: Seq[Team],
  ): VdomElement = {
    component(
      Props(
        showMasterData = showMasterData,
        quizState = quizState,
        teams = teams,
      )
    )
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      showMasterData: Boolean,
      quizState: QuizState = QuizState.nullInstance,
      teams: Seq[Team] = Seq(),
  ) {
    def question: Question = quizState.maybeQuestion.get
    def round: Round = quizState.round
    def questionProgressIndex: Int = quizState.questionProgressIndex
  }

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val _1: Props = props
      <.div(
        ^.className := "question-wrapper",
        props.questionProgressIndex match {
          case 0 if !props.showMasterData => showPreparatoryTitle(props.question)
          case _ =>
            props.question match {
              case single: Question.Standard                 => showSingleQuestion(single)
              case multipleAnswers: Question.MultipleAnswers => showMultipleAnswersQuestion(multipleAnswers)
              case double: Question.DoubleQ                  => showDoubleQuestion(double)
              case orderItems: Question.OrderItems           => showOrderItemsQuestion(orderItems)
            }
        },
      )
    }

    private def showPreparatoryTitle(question: Question)(implicit props: Props): VdomElement = {
      val questionNumber = (props.round.questions indexOf question) + 1
      <.div(
        <.div(
          ^.className := "question",
          i18n("app.question"),
          " ",
          questionNumber,
        ),
        SubComponents.pointsMetadata(question),
      )
    }

    private def showSingleQuestion(
        question: Question.Standard
    )(implicit
        props: Props
    ): VdomElement = {
      implicit val _ = props.quizState
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)
      val showSubmissionsOnChoices =
        question.isMultipleChoice && (question.onlyFirstGainsPoints || answerIsVisible)
      val showGamepadIconUnderChoices =
        props.quizState.submissions.nonEmpty || (props.quizState.canAnyTeamSubmitResponse && question.onlyFirstGainsPoints)
      val maybeImage = if (answerIsVisible) question.answerImage orElse question.image else question.image

      <.div(
        SubComponents.questionTitle(question),
        SubComponents.questionDetail(question),
        SubComponents.pointsMetadata(question),
        SubComponents.masterNotes(question),
        <.div(
          ^.className := "image-and-choices-row",
          <<.ifDefined(maybeImage) { image =>
            ifVisibleOrMaster(progressIndex > 0) {
              <.div(
                ^.className := "image-holder",
                ^.className := image.size,
                <.img(
                  ^.src := s"/quizassets/${JsQuizAssets.encodeSource(image.src)}",
                  ^.className := image.size,
                  ^^.ifThen(props.quizState.imageIsEnlarged) {
                    if (props.showMasterData) {
                      ^.className := "indicate-enlarged"
                    } else {
                      ^.className := "enlarged"
                    }
                  },
                ),
              )
            }
          },
          <<.ifDefined(question.videoSrc) { videoSrc =>
            ifVisibleOrMaster(question.submissionAreOpen(props.questionProgressIndex)) {
              val timerState = props.quizState.timerState
              val timerIsRunning = timerState.timerRunning && !timerState
                .hasFinished(question.maxTime) && question.submissionAreOpen(props.questionProgressIndex)
              <.div(
                ^.className := "video-holder",
                ^^.ifThen(props.quizState.imageIsEnlarged) {
                  if (props.showMasterData) {
                    ^.className := "indicate-enlarged"
                  } else {
                    ^.className := "enlarged"
                  }
                },
                if (props.showMasterData) {
                  SubComponents.videoHelpPlaceholder(
                    videoSrc,
                    playing = timerIsRunning,
                  )
                } else {
                  SubComponents.videoPlayer(
                    videoSrc,
                    playing = timerIsRunning,
                    key = props.quizState.timerState.uniqueIdOfMediaPlaying.toString,
                  )
                },
              )
            }
          },
          <<.ifDefined(question.choices) { choices =>
            ifVisibleOrMaster(question.choicesAreVisible(progressIndex)) {
              <.div(
                ^.className := "choices-holder",
                ^^.ifThen(maybeImage.isDefined || question.videoSrc.isDefined) {
                  ^.className := "including-image-or-video"
                },
                <.ul(
                  ^.className := "choices",
                  (for ((choice, answerBullet) <- choices zip AnswerBullet.all)
                    yield {
                      val visibleSubmissions =
                        if (showSubmissionsOnChoices)
                          props.quizState.submissions
                            .filter(_.value == SubmissionValue.MultipleChoiceAnswer(answerBullet.answerIndex))
                        else Seq()
                      val isCorrectAnswer =
                        question.isCorrectAnswer(
                          SubmissionValue.MultipleChoiceAnswer(answerBullet.answerIndex)
                        ) == Some(true)
                      <.li(
                        ^.key := choice,
                        answerBullet.toVdomNode,
                        if (isCorrectAnswer && (answerIsVisible || visibleSubmissions.nonEmpty)) {
                          <.span(^.className := "correct", choice)
                        } else if (!isCorrectAnswer && visibleSubmissions.nonEmpty) {
                          <.span(^.className := "incorrect", choice)
                        } else {
                          choice
                        },
                        " ",
                        showSubmissions(visibleSubmissions),
                      )
                    }).toVdomArray,
                ),
              )
            }
          },
        ),
        <.div(
          ^.className := "submissions-without-choices",
          ifVisibleOrMaster(showGamepadIconUnderChoices) {
            Bootstrap.FontAwesomeIcon("gamepad")
          },
          " ",
          <<.ifThen(!showSubmissionsOnChoices) {
            showSubmissions(props.quizState.submissions)
          },
        ),
        <<.ifDefined(question.audioSrc) { audioRelativePath =>
          <<.ifThen(question.submissionAreOpen(props.questionProgressIndex) && !props.showMasterData) {
            val timerState = props.quizState.timerState
            val timerIsRunning = timerState.timerRunning && !timerState.hasFinished(question.maxTime)
            SubComponents.audioPlayer(
              audioRelativePath,
              playing = timerIsRunning,
              key = props.quizState.timerState.uniqueIdOfMediaPlaying.toString,
            )
          }
        },
        <<.ifThen(question.choices.isEmpty || !answerIsVisible) {
          SubComponents.answerIfVisible(question)
        },
        SubComponents.answerDetailIfVisible(question),
      )
    }

    private def showMultipleAnswersQuestion(
        question: Question.MultipleAnswers
    )(implicit props: Props): VdomElement = {
      implicit val _ = props.quizState
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)
      val maybeImage = if (answerIsVisible) question.answerImage orElse question.image else question.image

      <.div(
        SubComponents.questionTitle(question),
        SubComponents.questionDetail(question),
        SubComponents.pointsMetadata(question),
        SubComponents.masterNotes(question),
        <.div(
          ^.className := "image-and-choices-row",
          <<.ifDefined(maybeImage) { image =>
            ifVisibleOrMaster(progressIndex > 0) {
              <.div(
                ^.className := "image-holder",
                ^.className := image.size,
                <.img(
                  ^.src := s"/quizassets/${JsQuizAssets.encodeSource(image.src)}",
                  ^.className := image.size,
                  ^^.ifThen(props.quizState.imageIsEnlarged) {
                    if (props.showMasterData) {
                      ^.className := "indicate-enlarged"
                    } else {
                      ^.className := "enlarged"
                    }
                  },
                ),
              )
            }
          },
          <<.ifDefined(question.videoSrc) { videoSrc =>
            ifVisibleOrMaster(question.submissionAreOpen(props.questionProgressIndex)) {
              val timerState = props.quizState.timerState
              val timerIsRunning = timerState.timerRunning && !timerState
                .hasFinished(question.maxTime) && question.submissionAreOpen(props.questionProgressIndex)
              <.div(
                ^.className := "video-holder",
                ^^.ifThen(props.quizState.imageIsEnlarged) {
                  if (props.showMasterData) {
                    ^.className := "indicate-enlarged"
                  } else {
                    ^.className := "enlarged"
                  }
                },
                if (props.showMasterData) {
                  SubComponents.videoHelpPlaceholder(
                    videoSrc,
                    playing = timerIsRunning,
                  )
                } else {
                  SubComponents.videoPlayer(
                    videoSrc,
                    playing = timerIsRunning,
                    key = props.quizState.timerState.uniqueIdOfMediaPlaying.toString,
                  )
                },
              )
            }
          },
        ),
        <.div(
          ^.className := "submissions-without-choices",
          showSubmissions(props.quizState.submissions),
        ),
        <<.ifDefined(question.audioSrc) { audioRelativePath =>
          <<.ifThen(question.submissionAreOpen(props.questionProgressIndex) && !props.showMasterData) {
            val timerState = props.quizState.timerState
            val timerIsRunning = timerState.timerRunning && !timerState.hasFinished(question.maxTime)
            SubComponents.audioPlayer(
              audioRelativePath,
              playing = timerIsRunning,
              key = props.quizState.timerState.uniqueIdOfMediaPlaying.toString,
            )
          }
        },
        SubComponents.answerIfVisible(question),
        SubComponents.answerDetailIfVisible(question),
      )
    }

    private def showDoubleQuestion(
        question: Question.DoubleQ
    )(implicit
        props: Props
    ): VdomElement = {
      implicit val _ = props.quizState
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)
      val correctSubmissionWasEntered =
        props.quizState.submissions.exists(s => s.isCorrectAnswer == Some(true))

      <.div(
        ifVisibleOrMaster(false) {
          <.div(
            <.div(
              ^.className := "verbal-question",
              <<.nl2BrBlockWithLinks(question.verbalQuestion),
            ),
            <.div(
              ^.className := "verbal-answer",
              <<.nl2BrBlockWithLinks(question.verbalAnswer),
            ),
          )
        },
        if (props.showMasterData) {
          ifVisibleOrMaster(question.questionIsVisible(progressIndex)) {
            <.div(
              ^.className := "textual-question",
              <<.nl2BrBlockWithLinks(question.textualQuestion),
            )
          }
        } else {
          <<.ifThen(question.questionIsVisible(progressIndex)) {
            <.div(
              ^.className := "question",
              <<.nl2BrBlockWithLinks(question.textualQuestion),
            )
          }
        },
        SubComponents.pointsMetadata(question),
        SubComponents.masterNotes(question),
        <.div(
          ^.className := "image-and-choices-row",
          ifVisibleOrMaster(question.choicesAreVisible(progressIndex)) {
            <.div(
              ^.className := "choices-holder",
              <.ul(
                ^.className := "choices",
                (for ((choice, answerBullet) <- question.textualChoices zip AnswerBullet.all)
                  yield {
                    val submissions =
                      props.quizState.submissions
                        .filter(_.value == SubmissionValue.MultipleChoiceAnswer(answerBullet.answerIndex))
                    val isCorrectAnswer =
                      question
                        .isCorrectAnswer(SubmissionValue.MultipleChoiceAnswer(answerBullet.answerIndex)) ==
                        Some(true)
                    <.li(
                      ^.key := choice,
                      answerBullet.toVdomNode,
                      if (
                        isCorrectAnswer && (answerIsVisible || submissions.nonEmpty || props.showMasterData)
                      ) {
                        <.span(^.className := "correct", choice)
                      } else if (!isCorrectAnswer && submissions.nonEmpty) {
                        <.span(^.className := "incorrect", choice)
                      } else {
                        choice
                      },
                      " ",
                      showSubmissions(submissions),
                    )
                  }).toVdomArray,
              ),
            )
          },
        ),
        <.div(
          ^.className := "submissions-without-choices",
          ifVisibleOrMaster(props.quizState.canAnyTeamSubmitResponse) {
            Bootstrap.FontAwesomeIcon("gamepad")
          },
        ),
      )
    }

    private def showOrderItemsQuestion(
        question: Question.OrderItems
    )(implicit
        props: Props
    ): VdomElement = {
      implicit val _ = props.quizState
      val progressIndex = props.questionProgressIndex
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)

      <.div(
        SubComponents.questionTitle(question),
        SubComponents.questionDetail(question),
        SubComponents.pointsMetadata(question),
        SubComponents.masterNotes(question),
        ifVisibleOrMaster(question.questionIsVisible(progressIndex)) {
          <.div(
            ^.className := "image-and-choices-row",
            <.div(
              ^.className := "choices-holder",
              <.ul(
                ^.className := "choices",
                if (answerIsVisible) {
                  (for (item <- question.orderedItemsThatWillBePresentedInAlphabeticalOrder)
                    yield {
                      <.li(
                        ^.key := s"Solution-${item.item}",
                        <.span(
                          ^.className := "correct",
                          s"${question.toCharacterCode(item)}/ ${item.item}",
                          <<.ifDefined(item.answerDetail) { answerDetail =>
                            s" ($answerDetail)"
                          },
                        ),
                      )
                    }).toVdomArray
                } else {
                  (for (item <- question.itemsInAlphabeticalOrder)
                    yield {
                      <.li(
                        ^.key := s"Item-${item.item}",
                        s"${question.toCharacterCode(item)}/ ${item.item}",
                      )
                    }).toVdomArray
                },
              ),
            ),
          )
        },
        <.div(
          ^.className := "submissions-without-choices",
          showSubmissions(props.quizState.submissions),
        ),
        SubComponents.answerIfVisible(question),
        SubComponents.answerDetailIfVisible(question),
      )
    }

  }

  private def ifVisibleOrMaster(isVisible: Boolean)(vdomTag: VdomTag)(implicit props: Props): VdomNode = {
    if (isVisible) {
      vdomTag
    } else if (props.showMasterData) {
      vdomTag(^.className := "admin-only-data")
    } else {
      VdomArray.empty()
    }
  }

  private def showSubmissions(submissions: Seq[Submission])(implicit props: Props) = {
    <<.joinWithSpaces(
      for {
        (submission, index) <- submissions.zipWithIndex
        team <- props.teams.find(_.id == submission.teamId)
      } yield TeamIcon(team)(
        ^.key := s"${submission.teamId}-$index"
      )
    )
  }

  private object SubComponents {

    def questionTitle(question: Question)(implicit props: Props): VdomNode = {
      ifVisibleOrMaster(question.questionIsVisible(props.questionProgressIndex)) {
        <.div(
          ^.className := "question",
          <<.ifDefined(question.tag) { tag =>
            Bootstrap.Label(BootstrapTags.toStableVariant(tag))(tag)
          },
          " ",
          <<.nl2BrBlockWithLinks(question.textualQuestion),
        )
      }
    }

    def questionDetail(question: Question)(implicit props: Props): VdomNode = {
      <<.ifDefined(question.questionDetail) { questionDetail =>
        ifVisibleOrMaster(question.questionIsVisible(props.questionProgressIndex)) {
          <.div(
            ^.className := "question-detail",
            <<.nl2BrBlockWithLinks(questionDetail),
          )
        }
      }
    }

    def pointsMetadata(question: Question): VdomElement = {
      val pointsToGainOnFirstAnswer = question.defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer = true)
      val pointsToGain = question.defaultPointsToGainOnCorrectAnswer(isFirstCorrectAnswer = false)
      <.div(
        ^.className := "points-metadata",
        if (question.onlyFirstGainsPoints) {
          if (pointsToGain == 1) i18n("app.first-right-answer-gains-1-point")
          else i18n("app.first-right-answer-gains-n-points", pointsToGain)
        } else {
          if (pointsToGainOnFirstAnswer != pointsToGain) {
            val gainN = {
              if (pointsToGainOnFirstAnswer == 1) i18n("app.first-right-answer-gains-1-point")
              else i18n("app.first-right-answer-gains-n-points", pointsToGainOnFirstAnswer)
            }
            val gainM = {
              if (pointsToGain == 1) i18n("app.others-gain-1-point")
              else i18n("app.others-gain-m-points", pointsToGain)
            }
            s"$gainN, $gainM"
          } else {
            if (pointsToGain == 1) i18n("app.all-right-answers-gain-1-point")
            else i18n("app.all-right-answers-gain-n-points", pointsToGain)
          }
        },
        <<.ifThen(question.defaultPointsToGainOnWrongAnswer != 0) {
          ". " + (
            if (question.defaultPointsToGainOnWrongAnswer == -1) i18n("app.wrong-answer-loses-1-point")
            else i18n("app.wrong-answer-loses-n-points", question.defaultPointsToGainOnWrongAnswer.negate)
          ) + "."
        },
      )
    }

    def masterNotes(question: Question)(implicit props: Props): VdomNode = {
      <<.ifDefined(question.masterNotes) { masterNotes =>
        ifVisibleOrMaster(false) {
          <.div(
            ^.className := "master-notes",
            <<.nl2BrBlockWithLinks(masterNotes),
          )
        }
      }
    }

    def answerIfVisible(question: Question)(implicit props: Props): VdomNode = {
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)

      ifVisibleOrMaster(answerIsVisible) {
        if (answerIsVisible) {
          <.div(
            ^.className := "answer",
            <<.nl2BrBlockWithLinks(question.answerAsString),
          )
        } else {
          <.div(obfuscatedAnswer(question.answerAsString))
        }
      }
    }

    def answerDetailIfVisible(question: Question)(implicit props: Props): VdomNode = {
      val answerIsVisible = question.answerIsVisible(props.questionProgressIndex)

      <<.ifThen(answerIsVisible) {
        <<.ifDefined(question.answerDetail) { answerDetail =>
          <.div(
            ^.className := "answer-detail",
            <<.nl2BrBlockWithLinks(answerDetail),
          )
        }
      }
    }

    /*private*/
    def audioPlayer(audioRelativePath: String, playing: Boolean, key: String): VdomNode = {
      RawMusicPlayer(
        src = s"/quizassets/${JsQuizAssets.encodeSource(audioRelativePath)}",
        playing = playing,
        key = key,
        showControls = true,
      )
    }

    /*private*/
    def videoPlayer(
        videoRelativePath: String,
        playing: Boolean,
        key: String,
    ): VdomNode = {
      RawVideoPlayer(
        src = s"/quizassets/${JsQuizAssets.encodeSource(videoRelativePath)}",
        playing = playing,
        key = key,
      )
    }

    /*private*/
    def videoHelpPlaceholder(
        videoRelativePath: String,
        playing: Boolean,
    ): VdomNode = {
      val playingString = if (playing) "playing" else "paused"
      <.div(
        ^.className := "video-help-placeholder",
        s"$videoRelativePath ($playingString)",
        <.br(),
        "Toggle playing: spacebar",
        <.br(),
        "Restart: shift + r",
        <.br(),
        "Toggle fullscreen: alt + enter",
      )
    }
  }
}
