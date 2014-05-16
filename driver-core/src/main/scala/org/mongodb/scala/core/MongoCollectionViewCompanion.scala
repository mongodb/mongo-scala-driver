package org.mongodb.scala.core

import org.mongodb.operation.Find
import org.mongodb.connection.{BufferProvider, Cluster}
import org.mongodb._

trait MongoCollectionViewCompanion[T] {

  this: RequiredTypesProvider =>

  val findOp: Find
  val writeConcern: WriteConcern
  val limitSet: Boolean
  val doUpsert: Boolean
  val readPreference: ReadPreference

  val client: Client
  val namespace: MongoNamespace
  val codec: CollectibleCodec[T]
  val options: MongoCollectionOptions

  def apply[D](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[D], options: MongoCollectionOptions) = {
    apply(client, namespace, codec, options, new Find(), writeConcern, limitSet = false, doUpsert = false, readPreference)
  }

  def apply[D](client: Client, namespace: MongoNamespace, codec: CollectibleCodec[D], options: MongoCollectionOptions,
               findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean,
               readPreference: ReadPreference): MongoCollectionViewProvider[D]
}
