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
package org.mongodb.scala.async.admin

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

import org.mongodb.{Document, Index}

import org.mongodb.scala.core.admin.MongoCollectionAdminProvider
import org.mongodb.scala.async.{CommandResponseHandler, MongoCollection, RequiredTypes}

case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends MongoCollectionAdminProvider[T]
  with CommandResponseHandler with RequiredTypes {

  override def drop(): ResultType[Unit] = dropRaw(result => result.mapTo[Unit])

  override def statistics: ResultType[Document] = statisticsRaw(result => result map {res => res.getResponse })

  override def isCapped: ResultType[Boolean] = statistics map { result => result.get("capped").asInstanceOf[Boolean] }

  override def getIndexes: ResultType[ListResultType[Document]] =
    getIndexesRaw(result => result map {docs => docs.asScala.toList})

  override def createIndexes(indexes: Iterable[Index]): ResultType[Unit] =
    createIndexesRaw(indexes, result => result.mapTo[Unit])

  override def dropIndex(index: String): ResultType[Unit] = dropIndexRaw(index, result => result.mapTo[Unit])
}

