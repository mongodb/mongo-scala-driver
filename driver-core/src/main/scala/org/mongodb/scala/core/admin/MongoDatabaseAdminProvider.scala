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

import org.bson.{BsonDocumentWrapper}

import org.mongodb.{Block, CommandResult, CreateCollectionOptions, Document, MongoAsyncCursor, MongoException, MongoFuture, MongoNamespace, ReadPreference}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.{CommandReadOperation, CreateCollectionOperation, Find, QueryOperation, RenameCollectionOperation, SingleResultFuture}

import org.mongodb.scala.core.{MongoDatabaseProvider, RequiredTypesAndTransformersProvider}

/**
 * The MongoDatabaseAdminProvider trait providing the core of a MongoDatabaseAdmin implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the concrete
 * implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation.
 *
 * {{{
 *    case class MongoDatabaseAdmin(database: MongoDatabase) extends MongoDatabaseAdminProvider
 *      with RequiredTypesAndTransformers
 * }}}
 */
trait MongoDatabaseAdminProvider {

  this: RequiredTypesAndTransformersProvider =>

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
  def drop(): ResultType[Unit] = {
    val operation = createOperation(DROP_DATABASE)
    val transformer = { result: MongoFuture[CommandResult] =>
      val future: SingleResultFuture[Unit] = new SingleResultFuture[Unit]
      result.register(new SingleResultCallback[CommandResult] {
        def onResult(result: CommandResult, e: MongoException): Unit = {
          future.init(Unit, e)
        }
      })
      future
    }
    client.executeAsync(operation, ReadPreference.primary, transformer).asInstanceOf[ResultType[Unit]]
  }

  /**
   * Gets all the collections in the database
   *
   * @return collection names
   */
  def collectionNames: ListResultType[String] = {
    val namespacesCollection: MongoNamespace = new MongoNamespace(name, "system.namespaces")
    val findAll = new Find()
    val operation = new QueryOperation[Document](namespacesCollection, findAll, commandCodec)
    val transformer = { result: MongoFuture[MongoAsyncCursor[Document]] =>
      val future: SingleResultFuture[List[String]] = new SingleResultFuture[List[String]]
      var list = List[String]()
      result.register(new SingleResultCallback[MongoAsyncCursor[Document]] {
        def onResult(cursor: MongoAsyncCursor[Document], e: MongoException): Unit = {
          cursor.forEach(new Block[Document] {
            override def apply(doc: Document): Unit = {
              doc.getString("name") match {
                case dollarCollectionName: String if dollarCollectionName.contains("$") => list
                case collectionName: String => list ::= collectionName.substring(database.name.length + 1)
              }
            }
          }).register(new SingleResultCallback[Void] {
            def onResult(result: Void, e: MongoException) {
              future.init(list.reverse, null)
            }
          })
        }
      })
      future
    }
    val results = client.executeAsync(operation, ReadPreference.primary, transformer)
    listToListResultTypeConverter[String](results.asInstanceOf[ResultType[List[String]]])
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

  private val name = database.name
  private val DROP_DATABASE = new Document("dropDatabase", 1)
  private val commandCodec = new DocumentCodec()
  private val client = database.client
  private def createOperation(command: Document) = {
    new CommandReadOperation(name, new BsonDocumentWrapper[Document](command, commandCodec))
  }

}

