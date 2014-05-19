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

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import org.mongodb.{Block, CommandResult, CreateCollectionOptions, Document, MongoAsyncCursor, MongoException, MongoNamespace, ReadPreference}
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.{CreateCollectionOperation, Find, QueryOperation, RenameCollectionOperation}

import org.mongodb.scala.core.admin.MongoDatabaseAdminProvider
import org.mongodb.scala.async.{CommandResponseHandler, MongoDatabase, RequiredTypes}

case class MongoDatabaseAdmin(database: MongoDatabase) extends MongoDatabaseAdminProvider with CommandResponseHandler with RequiredTypes {

  def drop(): Future[CommandResult] = {
    val futureDrop: Future[CommandResult] = database.executeAsyncWriteCommand(DROP_DATABASE)
    handleNameSpaceErrors(futureDrop)
  }

  def collectionNames: Future[List[String]] = {
    val namespacesCollection: MongoNamespace = new MongoNamespace(name, "system.namespaces")
    val findAll = new Find()
    val lengthOfDatabaseName = name.length()
    val operation = new QueryOperation[Document](namespacesCollection, findAll, commandCodec, commandCodec)

    val promise = Promise[List[String]]()
    var list = List[String]()
    val futureCursor = client.executeAsync(operation, ReadPreference.primary).asInstanceOf[Future[MongoAsyncCursor[Document]]]
    futureCursor.onComplete({
      case Success(cursor) =>
        cursor.forEach(new Block[Document] {
          override def apply(doc: Document): Unit = {
            doc.getString("name") match {
              case dollarCollectionName: String if dollarCollectionName.contains("$") => list
              case collectionName: String => list ::= collectionName.substring(lengthOfDatabaseName + 1)
            }
          }
        }).register(new SingleResultCallback[Void] {
          def onResult(result: Void, e: MongoException) {
            promise.success(list.reverse)
          }
        })
      case Failure(e) => promise.failure(e)
    })
    promise.future
  }

  def createCollection(createCollectionOptions: CreateCollectionOptions): Future[Unit] =
    client.executeAsync(new CreateCollectionOperation(name, createCollectionOptions)).asInstanceOf[Future[Unit]]

  def renameCollection(operation: RenameCollectionOperation): Future[Unit] =
    client.executeAsync(operation).asInstanceOf[Future[Unit]]


}
