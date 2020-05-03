package hydro.models.access

import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future

final class InMemoryLocalDatabase extends LocalDatabase {

  private var inMemoryEntityDatabase: InMemoryEntityDatabase = new InMemoryEntityDatabase()
  private val _pendingModifications: mutable.Buffer[EntityModification] = mutable.Buffer()
  private val singletonMap: mutable.Map[SingletonKey[_], Any] = mutable.Map()

  // **************** Getters ****************//
  override def queryExecutor[E <: Entity: EntityType](): DbQueryExecutor.Async[E] = {
    inMemoryEntityDatabase.queryExecutor[E].asAsync
  }
  override def pendingModifications(): Future[Seq[EntityModification]] = {
    Future.successful(_pendingModifications.toVector)
  }
  override def getSingletonValue[V](key: SingletonKey[V]): Future[Option[V]] = {
    Future.successful(singletonMap.get(key).asInstanceOf[Option[V]])
  }
  override def isEmpty: Future[Boolean] = {
    Future.successful(singletonMap.isEmpty)
  }

  // **************** Setters ****************//
  override def applyModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    Future.successful(modifications.foreach(inMemoryEntityDatabase.update))
  }
  override def addAll[E <: Entity: EntityType](entities: Seq[E]): Future[Unit] = {
    applyModifications(entities.map(EntityModification.Add[E]))
  }

  override def addPendingModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    Future.successful(_pendingModifications ++= modifications)
  }
  override def removePendingModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    Future.successful(_pendingModifications --= modifications)
  }

  override def setSingletonValue[V](key: SingletonKey[V], value: V): Future[Unit] = {
    singletonMap.put(key, value)
    Future.successful((): Unit)
  }

  override def save(): Future[Unit] = {
    Future.successful((): Unit)
  }

  override def resetAndInitialize(): Future[Unit] = {
    inMemoryEntityDatabase = new InMemoryEntityDatabase()
    _pendingModifications.clear()
    singletonMap.clear()
    Future.successful((): Unit)
  }
}
