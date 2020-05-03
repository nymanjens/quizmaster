package hydro.models.access

import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.concurrent.Future

final class InMemoryLocalDatabase extends LocalDatabase {

  private val inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase()

  // **************** Getters ****************//
  override def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E] = {
    inMemoryEntityDatabase.queryExecutor[E].asAsync
  }
  override def pendingModifications(): Future[Seq[EntityModification]] = ???
  override def getSingletonValue[V](key: SingletonKey[V]): Future[Option[V]] = ???
  override def isEmpty: Future[Boolean] = ???

  // **************** Setters ****************//
  override def applyModifications(modifications: Seq[EntityModification]): Future[Unit] = ???
  override def addAll[E <: Entity: EntityType](entities: Seq[E]): Future[Unit] = ???

  override def addPendingModifications(modifications: Seq[EntityModification]): Future[Unit] = ???
  override def removePendingModifications(modifications: Seq[EntityModification]): Future[Unit] = ???

  override def setSingletonValue[V](key: SingletonKey[V], value: V): Future[Unit] = ???

  override def save(): Future[Unit] = ???

  override def resetAndInitialize(): Future[Unit] = ???
}
