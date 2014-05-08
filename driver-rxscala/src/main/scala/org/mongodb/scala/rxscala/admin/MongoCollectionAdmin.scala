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
package org.mongodb.scala.rxscala.admin

import java.util

import scala.collection.JavaConverters._

import org.mongodb._
import org.mongodb.operation.{CreateIndexesOperation, DropCollectionOperation, DropIndexOperation, GetIndexesOperation}

import org.mongodb.scala.rxscala.MongoCollection
import org.mongodb.scala.rxscala.utils.HandleCommandResponse

import rx.lang.scala.Observable

case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends HandleCommandResponse {

  private val COLLECTION_STATS = new Document("collStats", collection.name)

  def drop(): Observable[Unit] = {
    val operation = new DropCollectionOperation(collection.namespace)
    collection.client.executeAsync(operation) map { result => Unit }
  }

  def isCapped: Observable[Boolean] = statistics map { result => result.get("capped").asInstanceOf[Boolean] }

  def statistics: Observable[Document] = {
    val futureStats: Observable[CommandResult] = collection.database.executeAsyncCommand(COLLECTION_STATS)
    handleNameSpaceErrors(futureStats) map { result => result.getResponse }
  }

  def getStatistics: Observable[Document] = statistics

  def createIndex(index: Index): Observable[Unit] = createIndexes(List(index))
  def createIndexes(indexes: Iterable[Index]): Observable[Unit] = {
    val operation = new CreateIndexesOperation(new util.ArrayList(indexes.toList.asJava), collection.namespace)
    collection.client.executeAsync(operation) map { v => Unit }
  }

  def getIndexes: Observable[Seq[Document]] = {
    val operation = new GetIndexesOperation(collection.namespace)
    collection.client.executeAsync(operation, collection.options.readPreference).map(docs => docs.asScala.toSeq)
  }

  def dropIndex(index: String): Observable[Unit] = {
    val operation = new DropIndexOperation(collection.namespace, index)
    collection.client.executeAsync(operation)  map { result => Unit }
  }

  def dropIndex(index: Index): Observable[Unit] = dropIndex(index.getName)

  def dropIndexes(): Observable[Unit] = dropIndex("*")

}
