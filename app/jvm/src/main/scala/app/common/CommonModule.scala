package app.common

import java.time.ZoneId

import com.google.inject._
import hydro.common.time.Clock
import hydro.common.time.JvmClock
import hydro.common.I18n
import hydro.common.PlayI18n

final class CommonModule extends AbstractModule {

  override def configure() = {
    bindSingleton(classOf[PlayI18n], classOf[PlayI18n.Impl])
    bind(classOf[I18n]).to(classOf[PlayI18n])
  }

  @Provides
  def provideClock(): Clock =
    // TODO: Make this configurable
    new JvmClock(ZoneId.of("Europe/Paris"))

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
