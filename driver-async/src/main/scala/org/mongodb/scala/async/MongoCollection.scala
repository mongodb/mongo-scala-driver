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
package org.mongodb.scala.async

import scala.Some
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import org.mongodb.{Block, CollectibleCodec, ConvertibleToDocument, Document, MongoAsyncCursor, MongoException,
                    MongoNamespace, QueryOptions, ReadPreference, WriteConcern, WriteResult}
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.{CountOperation, Find, FindAndRemove, FindAndRemoveOperation, FindAndReplace,
                              FindAndReplaceOperation, FindAndUpdate, FindAndUpdateOperation, InsertOperation,
                              InsertRequest, QueryOperation, RemoveOperation, RemoveRequest, ReplaceOperation,
                              ReplaceRequest, UpdateOperation, UpdateRequest}

import org.mongodb.scala.core.{MongoCollectionViewProvider, MongoCollectionProvider, MongoCollectionOptions}
import org.mongodb.scala.async.admin.MongoCollectionAdmin
import org.mongodb.scala.async.utils.HandleCommandResponse
import org.mongodb.scala.core.admin.MongoCollectionAdminProvider

/**
 * A MongoDB Collection
 *
 * @param name The name of the collection
 * @param database The database that this collection belongs to
 * @param codec The collectable codec to use for operations
 * @param options The options to use with this collection
 * @tparam T
 */
case class MongoCollection[T](name: String,
                               database: MongoDatabase,
                               codec: CollectibleCodec[T],
                               options: MongoCollectionOptions) extends MongoCollectionProvider[T] with RequiredTypes {


  override protected def collectionView: MongoCollectionView[T] = {
    MongoCollectionView[T](client.asInstanceOf[MongoClient], namespace, codec, options, new Find(), options.writeConcern, limitSet=false,
                           doUpsert=false, options.readPreference)
  }

  override val admin: MongoCollectionAdmin[T] = MongoCollectionAdmin(this)

}
