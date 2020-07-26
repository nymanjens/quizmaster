package hydro.flux.react

import hydro.common.GuavaReplacement.Splitter
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq
import scala.scalajs.js

object ReactVdomUtils {
  object ^^ {
    def classes(cls: String*): TagMod = classes(cls.toVector)
    def classes(cls: Iterable[String]): TagMod =
      ^.classSetM(cls.toVector.filter(_.nonEmpty).map(c => (c, true)).toMap)

    def ifThen(cond: Boolean)(thenElement: => TagMod): TagMod = {
      if (cond) {
        thenElement
      } else {
        TagMod.empty
      }
    }
    def ifDefined[T](option: Option[T])(thenElement: T => TagMod): TagMod = {
      ifThen(option.isDefined)(thenElement(option.get))
    }
  }

  object << {
    def ifThen(cond: Boolean)(thenElement: => VdomNode): VdomNode = {
      if (cond) {
        thenElement
      } else {
        VdomArray.empty()
      }
    }

    def ifDefined[T](option: Option[T])(thenElement: T => VdomNode): VdomNode = {
      ifThen(option.isDefined)(thenElement(option.get))
    }

    def joinWithSpaces[A](elems: TraversableOnce[A])(
        implicit f: A => VdomNode,
        stringF: String => VdomNode,
    ): VdomArray = {
      VdomArray.empty() ++= elems.flatMap(a => Seq(f(a), stringF(" ")))
    }

    def nl2Br(string: String): VdomNode = {
      val parts =
        Splitter
          .on('\n')
          .trimResults()
          .split(string.trim)
          .map(s => Seq[VdomNode](s))
          .reduce((seq1, seq2) => Seq(seq1, Seq[VdomNode](<.br()), seq2).flatten)
      VdomArray.apply(parts: _*)
    }

    def nl2BrBlock(string: String): VdomNode = {
      if (string contains '\n') {
        <.div(
          ^.style := js.Dictionary(
            "display" -> "inline-block",
            "textAlign" -> "left",
          ),
          nl2Br(string),
        )
      } else {
        string
      }
    }
  }
}
