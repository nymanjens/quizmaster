package app.common

/** Number with a single decimal digit after the point. This is used for representing points/sores. */
class FixedPointNumber private (
    private[FixedPointNumber] val numberWithoutPoint: Int,
) {

  override def toString: String = {
    if (numberWithoutPoint % 10 == 0) {
      (numberWithoutPoint / 10).toString
    } else {
      (numberWithoutPoint / 10.0).toString
    }
  }

  override def equals(o: Any): Boolean = o match {
    case that: FixedPointNumber => this.numberWithoutPoint == that.numberWithoutPoint
    case _                      => false
  }
  override def hashCode(): Int = numberWithoutPoint

  def +(that: FixedPointNumber): FixedPointNumber = {
    new FixedPointNumber(this.numberWithoutPoint + that.numberWithoutPoint)
  }
  def +(that: Int): FixedPointNumber = {
    this + FixedPointNumber(that)
  }
  def -(that: FixedPointNumber): FixedPointNumber = {
    new FixedPointNumber(this.numberWithoutPoint - that.numberWithoutPoint)
  }
  def -(that: Int): FixedPointNumber = {
    this - FixedPointNumber(that)
  }
  def >(that: FixedPointNumber): Boolean = {
    this.numberWithoutPoint > that.numberWithoutPoint
  }
  def >(that: Int): Boolean = {
    this > FixedPointNumber(that)
  }
  def <(that: FixedPointNumber): Boolean = {
    this.numberWithoutPoint < that.numberWithoutPoint
  }
  def <(that: Int): Boolean = {
    this < FixedPointNumber(that)
  }

  def ==(that: Int): Boolean = {
    this == FixedPointNumber(that)
  }

  def !=(that: Int): Boolean = {
    this != FixedPointNumber(that)
  }

  def toDouble: Double = numberWithoutPoint / 10.0
}

object FixedPointNumber {
  def apply(double: Double): FixedPointNumber = {
    new FixedPointNumber((double * 10).round.toInt)
  }

  def apply(int: Int): FixedPointNumber = {
    new FixedPointNumber(int * 10)
  }

  def unapply(fpn: FixedPointNumber): Option[Int] = {
    if (fpn.numberWithoutPoint % 10 == 0) {
      Some(fpn.numberWithoutPoint / 10)
    } else {
      None
    }
  }

  implicit object FixedPointNumberNumeric extends Numeric[FixedPointNumber] {
    override def plus(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = x + y
    override def minus(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = x - y
    override def times(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = ???
    override def negate(x: FixedPointNumber): FixedPointNumber = FixedPointNumber(0) - x
    override def fromInt(x: Int): FixedPointNumber = FixedPointNumber(x)
    override def toInt(x: FixedPointNumber): Int = ???
    override def toLong(x: FixedPointNumber): Long = ???
    override def toFloat(x: FixedPointNumber): Float = ???
    override def toDouble(x: FixedPointNumber): Double = x.toDouble
    override def compare(x: FixedPointNumber, y: FixedPointNumber): Int = x.numberWithoutPoint compare y.numberWithoutPoint
  }
}
