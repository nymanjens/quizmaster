package app.models

import app.models.access.JvmEntityAccess
import com.google.inject.AbstractModule
import hydro.models.access.EntityAccess

final class ModelsModule extends AbstractModule {
  override def configure() = {
    bindSingleton(classOf[EntityAccess], classOf[JvmEntityAccess])
  }

  private def bindSingleton[T](interface: Class[T], implementation: Class[_ <: T]): Unit = {
    bind(interface).to(implementation)
    bind(implementation).asEagerSingleton
  }
}
