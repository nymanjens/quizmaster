package app.controllers

import app.controllers.helpers.ScalaJsApiCallerImpl
import com.google.inject.AbstractModule
import hydro.controllers.InternalApi.ScalaJsApiCaller

final class ControllersModule extends AbstractModule {

  override def configure() = {
    bind(classOf[ScalaJsApiCaller]).to(classOf[ScalaJsApiCallerImpl])
  }
}
