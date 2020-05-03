package hydro.models.access

import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq

final class InMemoryLocalDatabase extends LocalDatabase {

  private val inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase()

  def queryExecutor[E <: Entity: EntityType]() = ???
  def pendingModifications() = ???
  def getSingletonValue[V](key: SingletonKey[V]) = ???
  def isEmpty = ???
  def applyModifications(modifications: Seq[EntityModification]) = ???
  def addAll[E <: Entity: EntityType](entities: Seq[E]) = ???
  def addPendingModifications(modifications: Seq[EntityModification]) = ???
  def removePendingModifications(modifications: Seq[EntityModification]) = ???
  def setSingletonValue[V](key: SingletonKey[V], value: V) = ???
  def save() = ???
  def resetAndInitialize() = ???

}
