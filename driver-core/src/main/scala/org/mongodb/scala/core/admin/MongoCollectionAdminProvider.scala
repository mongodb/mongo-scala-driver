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
import org.mongodb.operation._

import org.mongodb.scala.core.{CommandResponseHandlerProvider, MongoCollectionProvider, RequiredTypesProvider}

trait MongoCollectionAdminProvider[T] {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  def drop(): ResultType[Unit]

  def isCapped: ResultType[Boolean]

  def statistics: ResultType[Document]

  def createIndex(index: Index): ResultType[Unit] = createIndexes(List(index))
  def createIndexes(indexes: Iterable[Index]): ResultType[Unit]

  def getIndexes: ListResultType[Document]

  def dropIndex(index: String): ResultType[Unit]

  def dropIndex(index: Index): ResultType[Unit] = dropIndex(index.getName)

  def dropIndexes(): ResultType[Unit] = dropIndex("*")

  protected val collection: MongoCollectionProvider[T]

  private val COLLECTION_STATS = new Document("collStats", collection.name)

  protected def dropHelper(transformer:  (ResultType[Void]) => ResultType[Unit]): ResultType[Unit] = {
    val operation = new DropCollectionOperation(collection.namespace)
    transformer(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  protected def statisticsHelper(transformer:  (ResultType[CommandResult]) => ResultType[Document]): ResultType[Document] = {
    val futureStats = collection.database.executeAsyncReadCommand(COLLECTION_STATS, collection.database.readPreference)
    transformer(handleNamedErrors(futureStats.asInstanceOf[ResultType[CommandResult]], Seq("not found")))
  }

  protected def createIndexesHelper(indexes: Iterable[Index], transformer:  (ResultType[Void]) => ResultType[Unit]): ResultType[Unit] = {
    val operation = new CreateIndexesOperation(new util.ArrayList(indexes.toList.asJava), collection.namespace)
    transformer(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  protected def getIndexesHelper(transformer: (ResultType[util.List[Document]]) => ListResultType[Document]): ListResultType[Document] = {
    val operation = new GetIndexesOperation(collection.namespace)
    transformer(
      collection.client.executeAsync(operation,
        collection.options.readPreference).asInstanceOf[ResultType[util.List[Document]]]
    )
  }

  protected def dropIndexHelper(index: String, transformer: (ResultType[Void]) => ResultType[Unit]) = {
    val operation = new DropIndexOperation(collection.namespace, index)
    transformer(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

}
