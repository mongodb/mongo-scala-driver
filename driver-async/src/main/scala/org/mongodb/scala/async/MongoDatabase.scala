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
package org.mongodb.scala.async

import scala.concurrent.Future

import org.mongodb.{Codec, CollectibleCodec, CommandResult, Document, ReadPreference}
import org.mongodb.codecs.{CollectibleDocumentCodec, ObjectIdGenerator}
import org.mongodb.operation.CommandReadOperation

import org.mongodb.scala.core.{MongoCollectionOptions, MongoDatabaseOptions}
import org.mongodb.scala.async.admin.MongoDatabaseAdmin

case class MongoDatabase(name: String, client: MongoClient, options: MongoDatabaseOptions) {

  def apply(collectionName: String): MongoCollection[Document] = collection(collectionName)

  def apply(collectionName: String, collectionOptions: MongoCollectionOptions): MongoCollection[Document] =
    collection(collectionName, collectionOptions)

  def collection(collectionName: String): MongoCollection[Document] =
    collection(collectionName, MongoCollectionOptions(options))

  def collection(collectionName: String, collectionOptions: MongoCollectionOptions): MongoCollection[Document] = {
    val codec = new CollectibleDocumentCodec(collectionOptions.primitiveCodecs, new ObjectIdGenerator())
    collection(collectionName, codec, collectionOptions)
  }

  def collection[T](collectionName: String, codec: CollectibleCodec[T]): MongoCollection[T] =
    collection(collectionName, codec, MongoCollectionOptions(options))

  def collection[T](collectionName: String, codec: CollectibleCodec[T], collectionOptions: MongoCollectionOptions): MongoCollection[T] =
    MongoCollection(collectionName, this, codec, collectionOptions)

  val admin: MongoDatabaseAdmin = MongoDatabaseAdmin(this)

  def documentCodec: Codec[Document] = options.documentCodec
  def readPreference: ReadPreference = options.readPreference

  def executeAsyncCommand(command: Document): Future[CommandResult] = executeAsyncCommand(command, readPreference)
  def executeAsyncCommand(command: Document, readPreference: ReadPreference): Future[CommandResult] = {
    client.executeAsync(createReadOperation(command), readPreference)
   }

  private def createReadOperation(command: Document) = {
    // scalastyle:off magic.number
    new CommandReadOperation(name, command, documentCodec, documentCodec)
    // scalastyle:on magic.number
  }
}
