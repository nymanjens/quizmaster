package app.common

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<
import app.flux.stores.quiz.GamepadStore.Arrow
import app.models.quiz.QuizState
import app.models.quiz.QuizState.GeneralQuizSettings.AnswerBulletType
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react.vdom.html_<^.VdomTag
import japgolly.scalajs.react.vdom.VdomNode

import scala.collection.immutable.Seq

final class AnswerBullet private (character: Char, val arrowIcon: VdomTag) {

  def answerIndex: Int = AnswerBullet.all.indexOf(this)

  def toVdomNode(implicit quizState: QuizState): VdomTag =
    quizState.generalQuizSettings.answerBulletType match {
      case AnswerBulletType.Arrows     => arrowIcon(^.className := "choice-arrow")
      case AnswerBulletType.Characters => <.span(s"$character/ ")
    }

}
object AnswerBullet {
  val all: Seq[AnswerBullet] = Seq(
    new AnswerBullet('A', Bootstrap.FontAwesomeIcon("chevron-circle-up")),
    new AnswerBullet('B', Bootstrap.FontAwesomeIcon("chevron-circle-right")),
    new AnswerBullet('C', Bootstrap.FontAwesomeIcon("chevron-circle-down")),
    new AnswerBullet('D', Bootstrap.FontAwesomeIcon("chevron-circle-left")),
    new AnswerBullet('E', Bootstrap.FontAwesomeIcon("question-circle")),
    new AnswerBullet('F', Bootstrap.FontAwesomeIcon("exclamation-circle")),
    new AnswerBullet('G', Bootstrap.FontAwesomeIcon("info-circle")),
    new AnswerBullet('H', Bootstrap.FontAwesomeIcon("check-circle")),
    new AnswerBullet('I', Bootstrap.FontAwesomeIcon("plus-circle")),
    new AnswerBullet('J', Bootstrap.FontAwesomeIcon("times-circle")),
  )
}
