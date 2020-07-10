package app.common

import app.common.FixedPointNumber.FixedPointNumberNumeric

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

  def toDouble: Double = FixedPointNumberNumeric.toDouble(this)
  def abs: FixedPointNumber = FixedPointNumberNumeric.abs(this)
  def negate: FixedPointNumber = FixedPointNumberNumeric.negate(this)

  def +(that: FixedPointNumber): FixedPointNumber = FixedPointNumberNumeric.plus(this, that)
  def +(that: Int): FixedPointNumber = this + FixedPointNumber(that)
  def -(that: FixedPointNumber): FixedPointNumber = FixedPointNumberNumeric.minus(this, that)
  def -(that: Int): FixedPointNumber = this - FixedPointNumber(that)

  def *(that: Int): FixedPointNumber = new FixedPointNumber(this.numberWithoutPoint * that)
  def *(that: Double): FixedPointNumber = FixedPointNumber(this.toDouble * that)
  def /(that: Int): FixedPointNumber = new FixedPointNumber(this.numberWithoutPoint / that)

  def >(that: FixedPointNumber): Boolean = FixedPointNumberNumeric.gt(this, that)
  def >(that: Int): Boolean = this > FixedPointNumber(that)
  def <(that: FixedPointNumber): Boolean = FixedPointNumberNumeric.lt(this, that)
  def <(that: Int): Boolean = this < FixedPointNumber(that)

  def ==(that: Int): Boolean = this == FixedPointNumber(that)
  def !=(that: Int): Boolean = this != FixedPointNumber(that)
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
    override def plus(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = {
      new FixedPointNumber(x.numberWithoutPoint + y.numberWithoutPoint)
    }
    override def minus(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = {
      new FixedPointNumber(x.numberWithoutPoint - y.numberWithoutPoint)
    }
    override def times(x: FixedPointNumber, y: FixedPointNumber): FixedPointNumber = ???
    override def negate(x: FixedPointNumber): FixedPointNumber = {
      new FixedPointNumber(-x.numberWithoutPoint)
    }
    override def fromInt(x: Int): FixedPointNumber = {
      FixedPointNumber(x)
    }
    override def toInt(x: FixedPointNumber): Int = ???
    override def toLong(x: FixedPointNumber): Long = ???
    override def toFloat(x: FixedPointNumber): Float = ???
    override def toDouble(x: FixedPointNumber): Double = {
      x.numberWithoutPoint / 10.0
    }
    override def compare(x: FixedPointNumber, y: FixedPointNumber): Int = {
      x.numberWithoutPoint compare y.numberWithoutPoint
    }
  }
}
