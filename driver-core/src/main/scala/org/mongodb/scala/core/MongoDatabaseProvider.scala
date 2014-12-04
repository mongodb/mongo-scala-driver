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

import com.mongodb.ReadPreference.primary
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.{ AsyncOperationExecutor, CreateCollectionOperation, DropDatabaseOperation, ListCollectionNamesOperation }
import org.bson.Document

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
 *    case class MongoDatabase(name: String, options: OperationOptions, executor: AsyncOperationExecutor)
 *      extends MongoDatabaseProvider with RequiredTypesAndTransformers {
 *
 *     def collection[T](collectionName: String, options: OperationOptions, executor: AsyncOperationExecutor)
 *
 *    }
 * }}}
 *
 */
trait MongoDatabaseProvider extends ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * The database name
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val name: String

  /**
   * The OperationOptions to be used with this MongoDatabase instance
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val options: OperationOptions

  /**
   * The AsyncOperationExecutor to be used with this MongoDatabase instance
   *
   * @note Its expected that the MongoDatabase implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoClient Implementation
   */
  val executor: AsyncOperationExecutor

  /**
   * Helper to get a collection
   *
   * @param collectionName the name of the collection
   * @return the collection
   */
  def apply(collectionName: String): Collection[Document] =
    collection(collectionName, options, executor, classOf[Document])

  /**
   * Helper to get a collection
   * @param collectionName  the name of the collection
   * @param collectionOptions  the options to use with the collection
   * @return the collection
   */
  def apply(collectionName: String, collectionOptions: OperationOptions): Collection[Document] =
    collection[Document](collectionName, collectionOptions.withDefaults(options), executor, classOf[Document])

  /**
   * An explicit helper to get a collection
   *
   * @param collectionName the name of the collection
   * @return the collection
   */
  def collection(collectionName: String): Collection[Document] =
    collection[Document](collectionName, options, executor, classOf[Document])

  /**
   * An explicit helper to get a collection
   *
   * @param collectionName the name of the collection
   * @return the collection
   */
  def collection(collectionName: String, operationOptions: OperationOptions): Collection[Document] =
    collection[Document](collectionName, operationOptions.withDefaults(options), executor, classOf[Document])

  /**
   * A concrete implementation of [[MongoCollectionProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   *
   * @param collectionName the name of the collection
   * @param operationOptions the options to use with the collection
   * @param executor the AsyncOperationExecutor to be used with this MongoDatabase instance
   * @param clazz the document return class type
   * @tparam T the document type
   * @return the collection
   */
  def collection[T](collectionName: String, operationOptions: OperationOptions, executor: AsyncOperationExecutor,
                    clazz: Class[T]): Collection[T]

  /**
   * Drops this database.
   */
  def dropDatabase(): ResultType[Unit] =
    executeAsync(new DropDatabaseOperation(name), voidToUnitResultTypeCallback())

  /**
   * Gets the names of all the collections in this database.
   */
  def collectionNames(): ListResultType[String] =
    executeAsync(new ListCollectionNamesOperation(name), primary, listToListResultTypeCallback[String]())

  /**
   * Create a new collection with the given name.
   *
   * @param collectionName the name for the new collection to create
   */
  def createCollection(collectionName: String): ResultType[Unit] =
    createCollection(collectionName, new CreateCollectionOptions)

  /**
   * Create a new collection with the selected options
   *
   * @param collectionName the name for the new collection to create
   * @param options        various options for creating the collection
   */
  def createCollection(collectionName: String, options: CreateCollectionOptions): ResultType[Unit] =
    executeAsync(new CreateCollectionOperation(name, collectionName)
      .capped(options.isCapped)
      .sizeInBytes(options.getSizeInBytes)
      .autoIndex(options.isAutoIndex)
      .maxDocuments(options.getMaxDocuments)
      .usePowerOf2Sizes(options.isUsePowerOf2Sizes), voidToUnitResultTypeCallback())
}
