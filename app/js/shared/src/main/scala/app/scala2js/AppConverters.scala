package app.scala2js

import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.scala2js.Scala2Js.MapConverter

object AppConverters {

  // **************** Convertor generators **************** //
  implicit def fromEntityType[E <: Entity: EntityType]: MapConverter[E] = ???
}
