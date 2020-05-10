package hydro.models.access

import app.models.modification.EntityTypes
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.UpdatableEntity

import scala.collection.immutable.Seq
import scala.collection.mutable

/** In memory storage class that supports DbQuery operations and EntityModifications. */
final class InMemoryEntityDatabase() {

  private val typeToCollection: InMemoryEntityDatabase.TypeToCollectionMap =
    new InMemoryEntityDatabase.TypeToCollectionMap()

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] = {
    val entityType = implicitly[EntityType[E]]
    typeToCollection(entityType)
  }

  def update(modification: EntityModification): Unit = {
    val entityType = modification.entityType
    typeToCollection(entityType).update(modification)
  }
}
object InMemoryEntityDatabase {

  private final class EntityCollection[E <: Entity: EntityType]() extends DbQueryExecutor.Sync[E] {

    private val idToEntityMap: mutable.Map[Long, E] = mutable.Map[Long, E]()

    def update(modification: EntityModification): Unit = {
      modification match {
        case EntityModification.Add(entity) =>
          if (!idToEntityMap.contains(entity.id)) {
            idToEntityMap.put(entity.id, entity.asInstanceOf[E])
          }
        case EntityModification.Update(entity) =>
          def updateInner[E2 <: E with UpdatableEntity] = {
            var previousValue: E2 = null.asInstanceOf[E2]
            val castEntity = entity.asInstanceOf[E2]
            if (idToEntityMap.contains(entity.id)) {
              previousValue = idToEntityMap(entity.id).asInstanceOf[E2]
              idToEntityMap.put(entity.id, UpdatableEntity.merge(previousValue, castEntity))
            }
          }
          updateInner
        case EntityModification.Remove(entityId) =>
          idToEntityMap.remove(entityId)
      }
    }

    // **************** DbQueryExecutor.Sync **************** //
    override def data(dbQuery: DbQuery[E]): Seq[E] = valuesAsStream(dbQuery).toVector
    override def count(dbQuery: DbQuery[E]): Int = valuesAsStream(dbQuery).size

    private def valuesAsStream(dbQuery: DbQuery[E]): Stream[E] = {
      def applySorting(stream: Stream[E]): Stream[E] = dbQuery.sorting match {
        case Some(sorting) => stream.sorted(sorting.toOrdering)
        case None          => stream
      }
      def applyLimit(stream: Stream[E]): Stream[E] = dbQuery.limit match {
        case Some(limit) => stream.take(limit)
        case None        => stream
      }

      var stream = idToEntityMap.valuesIterator.toStream
      stream = stream.filter(dbQuery.filter.apply)
      stream = applySorting(stream)
      stream = applyLimit(stream)
      stream
    }
  }
  private object EntityCollection {
    type any = EntityCollection[_ <: Entity]
  }

  private final class TypeToCollectionMap() {
    private val typeToCollection: Map[EntityType.any, EntityCollection.any] = {
      for (entityType <- EntityTypes.fullySyncedLocally) yield {
        def internal[E <: Entity](
            implicit entityType: EntityType[E]): (EntityType.any, EntityCollection.any) = {
          entityType -> new EntityCollection[E]()
        }
        internal(entityType)
      }
    }.toMap

    def apply[E <: Entity](entityType: EntityType[E]): EntityCollection[E] =
      typeToCollection(entityType).asInstanceOf[EntityCollection[E]]
  }
}
