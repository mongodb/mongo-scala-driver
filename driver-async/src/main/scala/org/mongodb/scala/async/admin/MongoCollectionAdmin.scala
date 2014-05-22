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
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.mongodb.{CommandResult, Document, Index}

import org.mongodb.scala.core.admin.MongoCollectionAdminProvider
import org.mongodb.scala.async.{CommandResponseHandler, MongoCollection, RequiredTypes}
import java.util

/**
 * MongoCollectionAdmin
 *
 * @inheritdoc
 * @param collection the MongoCollection instance
 * @tparam T the type of collection
 */
case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends MongoCollectionAdminProvider[T]
  with CommandResponseHandler with RequiredTypes {

  /**
   * A type transformer that takes a `Future[Void]` and converts it to `Future[Unit]`
   *
   * @return Future[Unit]
   */
  protected def voidToUnitConverter: Future[Void] => Future[Unit] = result => result.mapTo[Unit]

  /**
   * A helper that gets the `capped` field from the statistics document
   *
   * @return Future[Boolean]
   */
  protected def getCappedFromStatistics: Future[Document] => Future[Boolean] = { result =>
    result map { doc => doc.get("capped").asInstanceOf[Boolean]}
  }

  /**
   * A helper that takes a `Future[CommandResult]` and picks out the `CommandResult.getResponse()` to return the
   * response Document as `Future[Document]`.
   *
   * @return Future[Document]
   */
  protected def getResponseHelper: Future[CommandResult] => Future[Document] = { result =>
    result map {cmdResult => cmdResult.getResponse }
  }

  /**
   * A type transformer that takes a `Future[util.List[Document\]\]` and converts it to `Future[List[Document\]\]`
   *
   * @return Future[List[Document\]\]
   */
  protected def javaListToListResultTypeConverter: Future[util.List[Document]] => Future[List[Document]] = {
    result => result map { docs => docs.asScala.toList }
  }
}

