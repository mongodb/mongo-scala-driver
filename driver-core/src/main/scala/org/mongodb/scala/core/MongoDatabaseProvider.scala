/**
 * Copyright (c) 2014 MongoDB, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * For questions and comments about this product, please see the project page at:
 *
 * https://github.com/mongodb/mongo-scala-driver
 */
package org.mongodb.scala.core

import org.mongodb.{ Document, ReadPreference }
import org.mongodb.codecs.{ CollectibleCodec, DocumentCodec }
import org.mongodb.operation.{ CommandReadOperation, CommandWriteOperation }

import org.mongodb.scala.core.admin.MongoDatabaseAdminProvider

import org.bson.{ BsonDocument, BsonDocumentWrapper }

/**
 * The MongoDatabaseProvider trait providing the core of a MongoDatabase implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the
 * concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation. To do this the concrete implementation of this trait requires
 * the following to be implemented:
 *
 * {{{
 *    case class MongoDatabase(name: String, client: MongoClient, options: MongoDatabaseOptions)
 *      extends MongoDatabaseProvider with RequiredTypesAndTransformers {
 *
 *      val admin: MongoDatabaseAdminProvider
 *
 *      def collection[T](collectionName: String, codec: CollectibleCodec[T],
 *                        collectionOptions: MongoCollectionOptions): Collection[T]
 *
 *    }
 * }}}
 *
 */
trait MongoDatabaseProvider {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * A concrete implementation of [[org.mongodb.scala.core.admin.MongoDatabaseAdminProvider MongoDatabaseAdminProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   */
  val admin: MongoDatabaseAdminProvider

  /**
   * The database name
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val name: String

  /**
   * The MongoClient instance used to create the MongoDatabase instance
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val client: MongoClientProvider

  /**
   * The MongoDatabaseOptions to be used with this MongoDatabase instance
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val options: MongoDatabaseOptions

  /**
   * Helper to get a collection
   *
   * @param collectionName the name of the collection
   * @return the collection
   */
  def apply(collectionName: String): Collection[Document] = collection(collectionName)

  /**
   * Helper to get a collection
   * @param collectionName  the name of the collection
   * @param collectionOptions  the options to use with the collection
   * @return the collection
   */
  def apply(collectionName: String, collectionOptions: MongoCollectionOptions): Collection[Document] =
    collection(collectionName, collectionOptions)

  /**
   * An explicit helper to get a collection
   *
   * @param collectionName the name of the collection
   * @return the collection
   */
  def collection(collectionName: String): Collection[Document] =
    collection(collectionName, MongoCollectionOptions(options))

  /**
   * An explicit helper to get a collection
   * @param collectionName  the name of the collection
   * @param collectionOptions  the options to use with the collection
   * @return the collection
   */
  def collection(collectionName: String, collectionOptions: MongoCollectionOptions): Collection[Document] = {
    val codec = new DocumentCodec()
    collection(collectionName, codec, collectionOptions)
  }

  /**
   * Helper to get a collection
   * @param collectionName  the name of the collection
   * @param codec  the codec to use with the collection
   * @return the collection
   */
  def collection[T](collectionName: String, codec: CollectibleCodec[T]): Collection[T] =
    collection(collectionName, codec, MongoCollectionOptions(options))

  /**
   * A concrete implementation of [[MongoCollectionProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   *
   * @param collectionName the name of the collection
   * @param codec the codec to use with the collection
   * @param collectionOptions the options to use with the collection
   * @tparam T the document type
   * @return the collection
   */
  def collection[T](collectionName: String, codec: CollectibleCodec[T],
                    collectionOptions: MongoCollectionOptions): Collection[T]

  private[scala] def executeAsyncWriteCommand(command: Document) = client.executeAsync(createWriteOperation(command))
  private[scala] def executeAsyncReadCommand(command: Document, readPreference: ReadPreference) =
    client.executeAsync(createReadOperation(command), readPreference)

  private def createWriteOperation(command: Document) =
    new CommandWriteOperation(name, wrap(command))

  private def createReadOperation(command: Document) =
    new CommandReadOperation(name, wrap(command))

  private def wrap(command: Document): BsonDocument = {
    new BsonDocumentWrapper[Document](command, options.documentCodec)
  }

}
