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

import org.mongodb.CollectibleCodec

import org.mongodb.scala.core.{MongoCollectionOptions, MongoDatabaseOptions, MongoDatabaseProvider}
import org.mongodb.scala.async.admin.MongoDatabaseAdmin
import org.mongodb.scala.core.admin.MongoDatabaseAdminProvider

case class MongoDatabase(name: String, client: MongoClient, options: MongoDatabaseOptions)
  extends MongoDatabaseProvider with RequiredTypes {

  /**
   * MongoDatabase administration functionality
   */
  val admin: MongoDatabaseAdmin = MongoDatabaseAdmin(this)

  /**
   * Provides a MongoCollection
   *
   * @param collectionName the name of the collection
   * @param codec the codec to use with the collection
   * @param collectionOptions the options to use with the collection
   * @tparam T the document type
   * @return the collection
   */
  def collection[T](collectionName: String, codec: CollectibleCodec[T],
                    collectionOptions: MongoCollectionOptions): MongoCollection[T] = {
    MongoCollection(collectionName, this, codec, collectionOptions)
  }
}
