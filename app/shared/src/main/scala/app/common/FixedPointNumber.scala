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

  def toDouble: Double = numberWithoutPoint / 10.0
}

object FixedPointNumber {
  def apply(double: Double): FixedPointNumber = {
    new FixedPointNumber((double * 10).round.toInt)
  }

  def apply(int: Int): FixedPointNumber = {
    new FixedPointNumber(int * 10)
  }
}
