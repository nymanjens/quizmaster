package app.common.testing
import java.time.Instant
import java.time.Month._

import app.api.ScalaJsApi.UpdateToken
import hydro.common.OrderToken
import hydro.common.time.LocalDateTime
import hydro.common.time.LocalDateTimes
import hydro.models.UpdatableEntity.LastUpdateTime

object TestObjects {

  def orderTokenA: OrderToken = OrderToken.middleBetween(None, Some(OrderToken.middle))
  def orderTokenB: OrderToken = OrderToken.middleBetween(Some(OrderToken.middle), None)
  def orderTokenC: OrderToken = OrderToken.middleBetween(Some(orderTokenB), None)
  def orderTokenD: OrderToken = OrderToken.middleBetween(Some(orderTokenC), None)
  def orderTokenE: OrderToken = OrderToken.middleBetween(Some(orderTokenD), None)
  def testOrderToken: OrderToken = orderTokenC

  def testDate: LocalDateTime = LocalDateTimes.createDateTime(2008, MARCH, 13)
  def testInstantA: Instant = Instant.ofEpochMilli(999000001)
  def testInstantB: Instant = Instant.ofEpochMilli(999000002)
  def testInstantC: Instant = Instant.ofEpochMilli(999000003)
  def testInstantD: Instant = Instant.ofEpochMilli(999000004)
  def testInstant: Instant = testInstantA
  def testUpdateToken: UpdateToken = s"123782:12378"

  def testLastUpdateTime = LastUpdateTime.allFieldsUpdated(testInstant)
}
