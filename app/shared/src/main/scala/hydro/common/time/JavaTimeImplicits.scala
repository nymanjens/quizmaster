package hydro.common.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

object JavaTimeImplicits {

  abstract class BaseWrapper[T: Ordering](thisComparable: T)(implicit ordering: Ordering[T]) {
    def <=(other: T): Boolean = ordering.compare(thisComparable, other) <= 0
    def <(other: T): Boolean = ordering.compare(thisComparable, other) < 0
    def >=(other: T): Boolean = ordering.compare(thisComparable, other) >= 0
    def >(other: T): Boolean = ordering.compare(thisComparable, other) > 0
  }

  implicit object InstantOrdering extends Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x compareTo y
  }
  implicit class InstantWrapper(thisInstant: Instant) extends BaseWrapper[Instant](thisInstant) {
    def -(duration: Duration): Instant = thisInstant minus duration
    def +(duration: Duration): Instant = thisInstant plus duration
    def -(instant: Instant): Duration = Duration.between(instant, thisInstant)
  }

  implicit class DurationWrapper(thisDuration: Duration) extends BaseWrapper[Duration](thisDuration) {
    def -(duration: Duration): Duration = thisDuration minus duration
    def +(duration: Duration): Duration = thisDuration plus duration
    def /(duration: Duration): Double = thisDuration.toNanos * 1.0 / duration.toNanos
  }

  implicit object LocalDateTimeOrdering extends Ordering[LocalDateTime] {
    override def compare(x: LocalDateTime, y: LocalDateTime): Int = x compareTo y
  }
  implicit class LocalDateTimeWrapper(thisDate: LocalDateTime) extends BaseWrapper[LocalDateTime](thisDate) {
    def -(other: LocalDateTime): Duration = {
      // Heuristic because scala.js doesn't support Duration.between(LocalDate, LocalDate)
      val localDateDayDiff = thisDate.toLocalDate.toEpochDay - other.toLocalDate.toEpochDay
      val localTimeNanoDiff = thisDate.toLocalTime.toNanoOfDay - other.toLocalTime.toNanoOfDay
      Duration.ofDays(localDateDayDiff) plus Duration.ofNanos(localTimeNanoDiff)
    }
  }

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x compareTo y
  }
  implicit class LocalDateWrapper(thisDate: LocalDate) extends BaseWrapper[LocalDate](thisDate)

//  implicit object DurationOrdering extends Ordering[Duration] {
//    override def compare(x: Duration, y: Duration): Int = x compareTo y
//  }
  implicit object DurationNumeric extends Numeric[Duration] {
    override def plus(x: Duration, y: Duration): Duration = x plus y
    override def minus(x: Duration, y: Duration): Duration = x minus y
    override def times(x: Duration, y: Duration): Duration = ???
    override def negate(x: Duration): Duration = x.negated()
    override def fromInt(x: Int): Duration = Duration.ofMillis(x)
    override def toInt(x: Duration): Int = ???
    override def toLong(x: Duration): Long = ???
    override def toFloat(x: Duration): Float = ???
    override def toDouble(x: Duration): Double = ???
    override def compare(x: Duration, y: Duration): Int = x compareTo y
  }
}
