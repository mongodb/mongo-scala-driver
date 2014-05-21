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

  val database: MongoDatabaseProvider

  def drop(): ResultType[CommandResult] = {
    handleNameSpaceErrors(database.executeAsyncWriteCommand(DROP_DATABASE).asInstanceOf[ResultType[CommandResult]])
  }

  def collectionNames: ListResultType[String]

  def createCollection(createCollectionOptions: CreateCollectionOptions): ResultType[Unit]

  def renameCollection(operation: RenameCollectionOperation): ResultType[Unit]

  def createCollection(collectionName: String): ResultType[Unit] = {
    createCollection(new CreateCollectionOptions(collectionName))
  }

  def renameCollection(oldCollectionName: String, newCollectionName: String): ResultType[Unit] = {
    renameCollection(oldCollectionName, newCollectionName, dropTarget = false)
  }

  def renameCollection(oldCollectionName: String, newCollectionName: String, dropTarget: Boolean): ResultType[Unit] = {
    val renameCollectionOptions = new RenameCollectionOperation(name, oldCollectionName, newCollectionName, dropTarget)
    renameCollection(renameCollectionOptions)
  }

  protected def collectionNamesHelper(f: CursorType[Document] => ListResultType[String]): ListResultType[String] = {
    val namespacesCollection: MongoNamespace = new MongoNamespace(name, "system.namespaces")
    val findAll = new Find()
    val operation = new QueryOperation[Document](namespacesCollection, findAll, commandCodec, commandCodec)
    f(client.executeAsync(operation, ReadPreference.primary).asInstanceOf[CursorType[Document]])
  }

  protected def createCollectionHelper(createCollectionOptions: CreateCollectionOptions,
                                       f: ResultType[Void] => ResultType[Unit]): ResultType[Unit] = {
    f(client.executeAsync(new CreateCollectionOperation(name, createCollectionOptions)).asInstanceOf[ResultType[Void]])
  }

  protected def renameCollectionHelper(operation: RenameCollectionOperation,
                                       f: ResultType[Void] => ResultType[Unit]): ResultType[Unit] = {
    f(client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  private val name = database.name
  private val DROP_DATABASE = new Document("dropDatabase", 1)
  private val commandCodec = new DocumentCodec()
  private val client = database.client
}

