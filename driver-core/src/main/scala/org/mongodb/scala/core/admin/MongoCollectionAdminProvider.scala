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
package org.mongodb.scala.core.admin

import java.util

import scala.collection.JavaConverters._

import org.mongodb.{CommandResult, Document, Index}
import org.mongodb.operation.{CreateIndexesOperation, DropCollectionOperation, DropIndexOperation, GetIndexesOperation}

import org.mongodb.scala.core.{CommandResponseHandlerProvider, MongoCollectionProvider, RequiredTypesProvider}


/**
 * The MongoCollectionAdminProvider trait providing the core of a MongoCollectionAdmin implementation.
 *
 * To use the trait it requires a concrete implementation of [CommandResponseHandlerProvider] and
 * [RequiredTypesProvider] to define handling of CommandResult errors and the types the concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesProvider] implementation. To do this the concrete implementation of this trait requires the following
 * methods to be implemented:
 *
 * {{{
 *    case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends MongoCollectionAdminProvider[T]
 *      with CommandResponseHandler with RequiredTypes {
 *
 *        protected def voidToUnitConverter: ResultType[Void] => ResultType[Unit]
 *
 *        protected def getResponseHelper: ResultType[CommandResult] => ResultType[Document]
 *
 *        protected def getCappedFromStatistics: ResultType[Document] => ResultType[Boolean]
 *
 *        protected def javaListToListResultTypeConverter: ResultType[util.List[Document]] => ListResultType[Document]
 * }}}
 *
 *
 * @tparam T the collection type
 */
trait MongoCollectionAdminProvider[T] {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  /**
   * Drops the collection
   *
   * @return ResultType[Unit]
   */
  def drop(): ResultType[Unit] = {
    val operation = new DropCollectionOperation(collection.namespace)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * Is the collection capped
   * @return ResultType[Boolean]
   */
  def isCapped: ResultType[Boolean] = getCappedFromStatistics(statistics)

  /**
   * Get statistics for the collection
   * @return ResultType[Document] of statistics
   */
  def statistics: ResultType[Document] = {
    val futureStats = collection.database.executeAsyncReadCommand(COLLECTION_STATS, collection.options.readPreference)
    getResponseHelper(handleNamedErrors(futureStats.asInstanceOf[ResultType[CommandResult]], Seq("not found")))
  }

  /**
   * Create an index on the collection
   * @param index the index to be created
   * @return ResultType[Unit]
   */
  def createIndex(index: Index): ResultType[Unit] = createIndexes(List(index))

  /**
   * Create multiple indexes on the collection
   * @param indexes an iterable of indexes
   * @return ResultType[Unit]
   */
  def createIndexes(indexes: Iterable[Index]): ResultType[Unit] = {
    val operation = new CreateIndexesOperation(new util.ArrayList(indexes.toList.asJava), collection.namespace)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * Get all the index information for this collection
   * @return ListResultType[Document]
   */
  def getIndexes: ListResultType[Document] = {
    val operation = new GetIndexesOperation(collection.namespace)
    javaListToListResultTypeConverter(
      collection.client.executeAsync(operation, collection.options.readPreference)
        .asInstanceOf[ResultType[util.List[Document]]]
    )
  }

  /**
   * Drop an index from the collection
   * @param index the index name to be dropped
   * @return ResultType[Unit]
   */
  def dropIndex(index: String): ResultType[Unit] = {
    val operation = new DropIndexOperation(collection.namespace, index)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   *  Drop an index from the collection
   *
   * @param index the `Index` instance to drop
   * @return ResultType[Unit]
   */
  def dropIndex(index: Index): ResultType[Unit] = dropIndex(index.getName)

  /**
   * Drop all indexes from this collection
   * @return ResultType[Unit]
   */
  def dropIndexes(): ResultType[Unit] = dropIndex("*")

  /**
   * The collection which we administrating
   *
   * @note Its expected that the MongoCollectionAdmin implementation is a case class and this is the constructor params.
   */
  val collection: MongoCollectionProvider[T]

  /**
   * A type transformer that takes a `ResultType[Void]` and converts it to `ResultType[Unit]`
   *
   * This is needed as returning `Void` is not idiomatic in scala whereas a `Unit` is more acceptable.
   *
   * For scala Futures an example is:
   * {{{
   *   result => result.mapTo[Unit]
   * }}}
   *
   * @note Each MongoCollectionAdmin implementation must provide this.
   *
   * @return ResultType[Unit]
   */
  protected def voidToUnitConverter: ResultType[Void] => ResultType[Unit]

  /**
   * A helper that takes a `ResultType[CommandResult]` and picks out the `CommandResult.getResponse()` to return the
   * response Document as `ResultType[Document]`.
   *
   * For Futures an example is:
   * {{{
   *   result => result map { cmdResult => cmdResult.getResponse }
   * }}}
   *
   * @note Each MongoCollectionAdmin implementation must provide this.
   *
   * @return ResultType[Document]
   */
  protected def getResponseHelper: ResultType[CommandResult] => ResultType[Document]

  /**
   * A helper that gets the `capped` field from the statistics document
   *
   * This is needed as we don't know before hand what shape or api a `ResultType[Document]` provides.
   *
   * For `Future[CommandResult] => Future[Document]`:
   * {{{
   *   result => result map { doc => doc.get("capped").asInstanceOf[Boolean] }
   * }}}
   *
   * @note Each MongoCollectionAdmin implementation must provide this.
   *
   * @return
   */
  protected def getCappedFromStatistics: ResultType[Document] => ResultType[Boolean]

  /**
   * A type transformer that takes a `ResultType[util.List[Document]]` and converts it to `ListResultType[Document]`
   *
   * For `Future[util.List[Document\]\] => Future[List[Document\]\]`:
   * {{{
   *   result => result map { docs => docs.asScala.toList }
   * }}}
   *
   * @return  ListResultType[Document]
   */
  protected def javaListToListResultTypeConverter: ResultType[util.List[Document]] => ListResultType[Document]

  /**
   * The Collection stats command document
   */
  private val COLLECTION_STATS = new Document("collStats", collection.name)

}
