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
package org.mongodb.scala.rxscala

import org.mongodb.CollectibleCodec
import org.mongodb.operation.Find

import org.mongodb.scala.core.{MongoCollectionOptions, MongoCollectionProvider}
import org.mongodb.scala.rxscala.admin.MongoCollectionAdmin

/**
 * A MongoDB Collection
 *
 * @param name The name of the collection
 * @param database The database that this collection belongs to
 * @param codec The collectable codec to use for operations
 * @param options The options to use with this collection
 * @tparam T the collection type (usually document)
 */
case class MongoCollection[T](name: String, database: MongoDatabase, codec: CollectibleCodec[T],
                              options: MongoCollectionOptions)
  extends MongoCollectionProvider[T] with RequiredTypesAndTransformers {

  /**
   * MongoCollection administration functionality
   */
  val admin: MongoCollectionAdmin[T] = MongoCollectionAdmin(this)

  /**
   * The MongoCollectionView to be used in chaining operations together
   * @return the MongoCollectionView
   */
  protected def collectionView: MongoCollectionView[T] = {
    MongoCollectionView[T](client.asInstanceOf[MongoClient], namespace, codec, options, new Find(), options.writeConcern, limitSet=false,
      doUpsert=false, options.readPreference)
  }

}
