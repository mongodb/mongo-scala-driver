package org.mongodb.scala.core

import org.mongodb.operation.Find
import org.mongodb.connection.{BufferProvider, Cluster}
import org.mongodb._

trait MongoCollectionViewCompanion {

  this: RequiredTypesProvider =>

  def apply[T](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T],
            options: MongoCollectionOptions): MongoCollectionViewProvider[T]

  def apply[T](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T], options: MongoCollectionOptions,
            findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean,
            readPreference: ReadPreference): MongoCollectionViewProvider[T]

}
