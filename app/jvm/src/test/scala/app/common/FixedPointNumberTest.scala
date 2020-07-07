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
}
