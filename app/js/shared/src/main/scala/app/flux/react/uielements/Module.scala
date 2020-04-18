package app.flux.react.uielements

import hydro.common.I18n
import hydro.flux.action.Dispatcher
import hydro.models.access.EntityAccess

final class Module(
    implicit i18n: I18n,
    entityAccess: EntityAccess,
    dispatcher: Dispatcher,
) {}
