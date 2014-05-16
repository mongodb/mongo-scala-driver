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

import org.mongodb.{CommandResult, CreateCollectionOptions, Document}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.operation.RenameCollectionOperation

import org.mongodb.scala.core.{RequiredTypesProvider, CommandResponseHandlerProvider, MongoDatabaseProvider}

trait MongoDatabaseAdminProvider {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  val database: MongoDatabaseProvider

  def drop(): ResultType[CommandResult]

  def collectionNames: ResultType[ListResultType[String]]

  def createCollection(createCollectionOptions: CreateCollectionOptions): ResultType[Unit]

  def renameCollection(operation: RenameCollectionOperation): ResultType[Unit]

  def createCollection(collectionName: String): ResultType[Unit] =
    createCollection(new CreateCollectionOptions(collectionName))


  def renameCollection(oldCollectionName: String, newCollectionName: String): ResultType[Unit] =
    renameCollection(oldCollectionName, newCollectionName, dropTarget = false)

  def renameCollection(oldCollectionName: String, newCollectionName: String, dropTarget: Boolean): ResultType[Unit] = {
    val renameCollectionOptions = new RenameCollectionOperation(name, oldCollectionName, newCollectionName, dropTarget)
    renameCollection(renameCollectionOptions)
  }

  protected val DROP_DATABASE = new Document("dropDatabase", 1)
  protected val name = database.name
  protected val commandCodec = new DocumentCodec()
  protected val client = database.client

}

