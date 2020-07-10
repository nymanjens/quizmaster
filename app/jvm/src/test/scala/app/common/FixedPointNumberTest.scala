package app.common

import org.junit.runner._
import org.specs2.mutable.Specification
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class FixedPointNumberTest extends Specification {

  "equals" in {
    FixedPointNumber(1.2) mustEqual FixedPointNumber(1.2)
    FixedPointNumber(1.0) mustEqual FixedPointNumber(1)

    FixedPointNumber(1.2) mustNotEqual FixedPointNumber(1.3)
    FixedPointNumber(1.2) mustNotEqual FixedPointNumber(1)
  }

  "toString" in {
    FixedPointNumber(1.0).toString mustEqual "1"
    FixedPointNumber(1.06).toString mustEqual "1.1"
    FixedPointNumber(1).toString mustEqual "1"
  }

  "toDouble" in {
    FixedPointNumber(1.0).toDouble mustEqual 1.0
    FixedPointNumber(1.6).toDouble mustEqual 1.6
    FixedPointNumber(1).toDouble mustEqual 1.0
  }

  "abs" in {
    FixedPointNumber(-1).abs mustEqual FixedPointNumber(1)
    FixedPointNumber(1.6).abs mustEqual FixedPointNumber(1.6)
  }

  "negate" in {
    FixedPointNumber(-1).negate mustEqual FixedPointNumber(1)
    FixedPointNumber(1.6).negate mustEqual FixedPointNumber(-1.6)
  }

  "+ and -" in {
    FixedPointNumber(1.2) + FixedPointNumber(2) mustEqual FixedPointNumber(3.2)
    FixedPointNumber(1.2) + 2 mustEqual FixedPointNumber(3.2)

    FixedPointNumber(1.2) - FixedPointNumber(2) mustEqual FixedPointNumber(-0.8)
    FixedPointNumber(1.2) - 2 mustEqual FixedPointNumber(-0.8)
  }

  "* and /" in {
    FixedPointNumber(1.2) * -2 mustEqual FixedPointNumber(-2.4)
    FixedPointNumber(1.2) * -2.0 mustEqual FixedPointNumber(-2.4)
    FixedPointNumber(1.2) * 0.5 mustEqual FixedPointNumber(0.6)

    FixedPointNumber(-1.2) / 2 mustEqual FixedPointNumber(-0.6)
  }

  "> and <" in {
    FixedPointNumber(1.2) < FixedPointNumber(2) mustEqual true
    FixedPointNumber(1.2) > FixedPointNumber(2) mustEqual false
    FixedPointNumber(2) < FixedPointNumber(-1.1) mustEqual false
    FixedPointNumber(2) > FixedPointNumber(-1.1) mustEqual true
  }

  "==  int" in {
    FixedPointNumber(1.0) == 1 mustEqual true
    FixedPointNumber(1.1) == 1 mustEqual false
    FixedPointNumber(0) == 0 mustEqual true

    FixedPointNumber(0) != 0 mustEqual false
    FixedPointNumber(0) != 1 mustEqual true
  }

  "sum" in {
    Seq(FixedPointNumber(1), FixedPointNumber(1.2)).sum mustEqual FixedPointNumber(2.2)
  }

  "unapply" in {
    (
      FixedPointNumber(1) match {
        case FixedPointNumber(1) => true
        case _                   => false
      }
    ) mustEqual true

    (
      FixedPointNumber(1.1) match {
        case FixedPointNumber(1) => false
        case _                   => true
      }
    ) mustEqual true
  }
}
