package org.mongodb.scala.core

import org.mongodb.{CollectibleCodec, MongoNamespace, ReadPreference, WriteConcern}
import org.mongodb.operation.Find

trait MongoCollectionViewCompanion {

  this: RequiredTypesProvider =>

  def apply[T](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T],
            options: MongoCollectionOptions): MongoCollectionViewProvider[T]

  def apply[T](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T], options: MongoCollectionOptions,
            findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean,
            readPreference: ReadPreference): MongoCollectionViewProvider[T]

}
