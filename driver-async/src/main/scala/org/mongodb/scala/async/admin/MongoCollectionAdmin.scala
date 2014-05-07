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
 *
 */
package org.mongodb.scala.async.admin

import java.util.ArrayList

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.mongodb._
import org.mongodb.operation.{CreateIndexesOperation, DropCollectionOperation, DropIndexOperation, GetIndexesOperation}
import org.mongodb.scala.async.MongoCollection
import org.mongodb.scala.async.utils.HandleCommandResponse

case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends HandleCommandResponse {

  private val COLLECTION_STATS = new Document("collStats", collection.name)

  def drop(): Future[Unit] = {
    val operation = new DropCollectionOperation(collection.namespace)
    collection.client.executeAsync(operation).mapTo[Unit]
  }

  def isCapped: Future[Boolean] = statistics map { result => result.get("capped").asInstanceOf[Boolean] }

  def statistics: Future[Document] = {
    val futureStats: Future[CommandResult] = collection.database.executeAsyncCommand(COLLECTION_STATS)
    handleNameSpaceErrors(futureStats) map { result => result.getResponse }
  }

  def getStatistics: Future[Document] = statistics

  def createIndex(index: Index): Future[Unit] = createIndexes(List(index))
  def createIndexes(indexes: Iterable[Index]): Future[Unit] = {
    val operation = new CreateIndexesOperation(new ArrayList(indexes.toList.asJava), collection.namespace)
    collection.client.executeAsync(operation).asInstanceOf[Future[Unit]]
  }

  def getIndexes: Future[Seq[Document]] = {
    val operation = new GetIndexesOperation(collection.namespace)
    collection.client.executeAsync(operation, collection.options.readPreference).map(docs => docs.asScala.toSeq)
  }

  def dropIndex(index: String): Future[Unit] = {
    val operation = new DropIndexOperation(collection.namespace, index)
    collection.client.executeAsync(operation).mapTo[Unit]
  }

  def dropIndex(index: Index): Future[Unit] = dropIndex(index.getName)

  def dropIndexes(): Future[Unit] = dropIndex("*")

}
