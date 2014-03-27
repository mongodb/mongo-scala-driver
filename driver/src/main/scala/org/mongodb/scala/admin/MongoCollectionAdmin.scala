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
package org.mongodb.scala.admin

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.mongodb._
import org.mongodb.operation.{CreateIndexesOperation, DropIndexOperation, GetIndexesOperation}

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.utils.HandleCommandResponse

case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends HandleCommandResponse {

  private lazy val name = collection.name
  private lazy val database = collection.database
  private lazy val client = collection.client
  private lazy val collectionNamespace = collection.namespace
  private lazy val COLLECTION_STATS = new Document("collStats",name)
  private lazy val DROP_COLLECTION = new Document("drop", name)

  def drop(): Future[CommandResult] = handleNameSpaceErrors(database.admin.executeAsync(DROP_COLLECTION))

  def isCapped: Future[Boolean] = statistics map { result => result.get("capped").asInstanceOf[Boolean] }

  def statistics: Future[Document] = {
    val stats = handleNameSpaceErrors(database.admin.executeAsync(COLLECTION_STATS))
    stats map { result => result.getResponse }
  }

  def getStatistics: Future[Document] = statistics

  def createIndex(index: Index): Future[Unit] = createIndexes(List(index))
  def createIndexes(indexes: Iterable[Index]): Future[Unit] = {
    val operation = new CreateIndexesOperation(indexes.toList.asJava, collectionNamespace,
                                               client.bufferProvider, client.session, false)
    // TODO needs executeAsync
    Future(operation.execute)
  }

  def getIndexes: Future[Seq[Document]] = {
    val operation = new GetIndexesOperation(collectionNamespace, client.bufferProvider, client.session)
    // TODO needs executeAsync
    Future(operation.execute.asScala.toSeq)
  }

  def dropIndex(index: String): Future[Document] = {
    val operation = new DropIndexOperation(collectionNamespace, index, client.bufferProvider, client.session, false)
    val namedErrors = Seq("index not found", "ns not found")
    // TODO needs executeAsync
    handleNamedErrors(Future(operation.execute), namedErrors) map { result => result.getResponse }
  }

  def dropIndex(index: Index): Future[Document] = dropIndex(index.getName)

  def dropIndexes(): Future[Document] = dropIndex("*")

}
