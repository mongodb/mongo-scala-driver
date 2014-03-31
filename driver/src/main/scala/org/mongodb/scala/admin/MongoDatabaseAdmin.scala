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

import java.util.concurrent.TimeUnit.SECONDS

import scala.concurrent._
import scala.language.implicitConversions

import org.mongodb.{CommandResult, CreateCollectionOptions, Document, MongoNamespace}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.connection.NativeAuthenticationHelper.createAuthenticationHash
import org.mongodb.operation._

import org.mongodb.scala.{MongoCollection, MongoDatabase}
import org.mongodb.scala.Implicits._
import org.mongodb.scala.utils.HandleCommandResponse

case class MongoDatabaseAdmin(database: MongoDatabase) extends HandleCommandResponse {

  private val DROP_DATABASE = new Document("dropDatabase", 1)
  private val name = database.name
  private val commandCodec = new DocumentCodec()
  private val client = database.client

  def drop(): Future[CommandResult] = {
    val futureDrop: Future[CommandResult] = database.executeAsyncCommand(DROP_DATABASE)
    handleNameSpaceErrors(futureDrop)
  }

  def collectionNames: Future[List[String]] = {
    val namespacesCollection: MongoNamespace = new MongoNamespace(name, "system.namespaces")
    val findAll = new Find().readPreference(org.mongodb.ReadPreference.primary)
    val lengthOfDatabaseName = name.length()

    val operation = new QueryOperation[Document](namespacesCollection, findAll, commandCodec, commandCodec)
    val collectionNames = client.executeAsyncRaw(operation)
    collectionNames.cursor.foldLeft(List[String]())({
      case (names, doc) =>
        doc.getString("name") match {
          case dollarCollectionName: String if dollarCollectionName.contains("$") => names
          case collectionName: String => collectionName.substring(lengthOfDatabaseName + 1) :: names
        }
    }).map({
      names => names.reverse
    }).toFuture
  }

  def createCollection(collectionName: String): Future[CommandResult] =
    createCollection(new CreateCollectionOptions(collectionName))

  def createCollection(createCollectionOptions: CreateCollectionOptions): Future[CommandResult] = {
    // scalastyle:off null magic.number
    val operation = new CommandOperation(name, createCollectionOptions.asDocument, null, commandCodec,
      commandCodec, client.cluster.getDescription(10, SECONDS))
    // scalastyle:on null magic.number
    handleErrors(client.executeAsync(operation))
  }

  def renameCollection(oldCollectionName: String, newCollectionName: String): Future[CommandResult] =
    renameCollection(oldCollectionName, newCollectionName, dropTarget = false)

  def renameCollection(oldCollectionName: String, newCollectionName: String, dropTarget: Boolean): Future[CommandResult] = {
    val renameCollectionOptions = new RenameCollectionOperation(name, oldCollectionName, newCollectionName, dropTarget)
    renameCollection(renameCollectionOptions)
  }

  def renameCollection(operation: RenameCollectionOperation): Future[CommandResult] =
    handleErrors(client.execute(operation))

  def addUser(userName: String, password: Array[Char], readOnly: Boolean) {
    // TODO - collection save
    // TODO - new Document
    val collection: MongoCollection[Document] = database("system.users")
    val doc: Document = new Document("user", userName)
      .append("pwd", createAuthenticationHash(userName, password))
      .append("readOnly", readOnly)
    // collection.save(doc)
  }

  def removeUser(userName: String) {
    // TODO - collection remove
    // TODO - new Document
    val collection: MongoCollection[Document] = database("system.users")
    // collection.filter(new Document("user", userName)).remove
  }

}
