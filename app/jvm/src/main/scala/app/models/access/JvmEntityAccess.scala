package app.models.access

import com.google.inject._
import hydro.common.time.Clock
import hydro.models.access.JvmEntityAccessBase

final class JvmEntityAccess @Inject() (implicit clock: Clock) extends JvmEntityAccessBase {}
