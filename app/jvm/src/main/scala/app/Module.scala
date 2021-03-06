import app.api.ScalaJsApiModule
import app.common.CommonModule
import app.controllers.ControllersModule
import app.models.ModelsModule
import app.models.quiz.config.ConfigModule
import com.google.inject.AbstractModule
import app.tools.ApplicationStartHook

final class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[ApplicationStartHook]).asEagerSingleton

    install(new CommonModule)
    install(new ConfigModule)
    install(new ControllersModule)
    install(new ModelsModule)
    install(new ScalaJsApiModule)
  }
}
