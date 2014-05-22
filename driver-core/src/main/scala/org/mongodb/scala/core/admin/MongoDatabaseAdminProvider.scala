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

import scala.language.higherKinds

import org.mongodb.{CommandResult, CreateCollectionOptions, Document, MongoNamespace, ReadPreference}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.operation.{CreateCollectionOperation, Find, QueryOperation, RenameCollectionOperation}

import org.mongodb.scala.core.{CommandResponseHandlerProvider, MongoDatabaseProvider, RequiredTypesProvider}

trait MongoDatabaseAdminProvider {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  /**
   * The database which we administrating
   *
   * @note Its expected that the MongoDatabaseAdmin implementation is a case class and this is the constructor params.
   */
  val database: MongoDatabaseProvider

  /**
   * Drop a collection
   *
   * @return the command result
   */
  def drop(): ResultType[CommandResult] = {
    handleNameSpaceErrors(database.executeAsyncWriteCommand(DROP_DATABASE).asInstanceOf[ResultType[CommandResult]])
  }

  /**
   * Gets all the collections in the database
   *
   * @return collection names
   */
  def collectionNames: ListResultType[String] = {
    val namespacesCollection: MongoNamespace = new MongoNamespace(name, "system.namespaces")
    val findAll = new Find()
    val operation = new QueryOperation[Document](namespacesCollection, findAll, commandCodec, commandCodec)
    collectionNamesHelper(client.executeAsync(operation, ReadPreference.primary).asInstanceOf[CursorType[Document]])
  }

  /**
   * Create a new collection
   * @param collectionName the name of the new collection
   * @return Unit
   */
  def createCollection(collectionName: String): ResultType[Unit] = {
    createCollection(new CreateCollectionOptions(collectionName))
  }

  /**
   * Create a new collection
   *
   * @param createCollectionOptions the specific options for the collection
   * @return Unit
   */
  def createCollection(createCollectionOptions: CreateCollectionOptions): ResultType[Unit] = {
    val operation = new CreateCollectionOperation(name, createCollectionOptions)
    voidToUnitConverter(client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * Rename a collection from one name to a new name
   *
   * @param oldCollectionName the old name
   * @param newCollectionName the new name
   * @return Unit
   */
  def renameCollection(oldCollectionName: String, newCollectionName: String): ResultType[Unit] = {
    renameCollection(oldCollectionName, newCollectionName, dropTarget = false)
  }

  /**
   * Rename a collection from one name to a new name
   *
   * @param oldCollectionName the old name
   * @param newCollectionName the new name
   * @param dropTarget drop the collection if it already exists
   * @return Unit
   */
  def renameCollection(oldCollectionName: String, newCollectionName: String, dropTarget: Boolean): ResultType[Unit] = {
    val renameCollectionOptions = new RenameCollectionOperation(name, oldCollectionName, newCollectionName, dropTarget)
    renameCollection(renameCollectionOptions)
  }

  /**
   * Rename a collection from one name to a new name
   *
   * @param operation the RenameCollection Operation
   * @return Unit
   */
  def renameCollection(operation: RenameCollectionOperation): ResultType[Unit] = {
    voidToUnitConverter(client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * A helper function that takes the collection name documents and returns a list of names from those documents
   *
   * This method should filter out any internal collection names (ones that contain a $)
   *
   * An example of `Future[MongoAsyncCursor[Document\]\] => Future[List[String\]\]` is:
   * {{{
   *   result =>
   *      val promise = Promise[List[String]]()
   *      var list = List[String]()
   *      result.onComplete({
   *        case Success(cursor) =>
   *          cursor.forEach(new Block[Document] {
   *            override def apply(doc: Document): Unit = {
   *              doc.getString("name") match {
   *                case dollarCollectionName: String if dollarCollectionName.contains("$") => list
   *                case collectionName: String => list ::= collectionName.substring(database.name.length + 1)
   *              }
   *            }
   *          }).register(new SingleResultCallback[Void] {
   *            def onResult(result: Void, e: MongoException) {
   *              promise.success(list.reverse)
   *            }
   *          })
   *        case Failure(e) => promise.failure(e)
   *      })
   *      promise.future
   * }}}
   *
   * @note Each MongoDatabaseAdmin implementation must provide this.
   *
   * @return the collection names
   */
  protected def collectionNamesHelper: CursorType[Document] => ListResultType[String]

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
   * @note Each MongoDatabaseAdmin implementation must provide this.
   *
   * @return ResultType[Unit]
   */
  protected def voidToUnitConverter: ResultType[Void] => ResultType[Unit]

  private val name = database.name
  private val DROP_DATABASE = new Document("dropDatabase", 1)
  private val commandCodec = new DocumentCodec()
  private val client = database.client
}

