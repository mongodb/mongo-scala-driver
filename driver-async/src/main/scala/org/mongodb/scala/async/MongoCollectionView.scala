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

import org.mongodb.{CollectibleCodec, MongoNamespace, ReadPreference, WriteConcern}
import org.mongodb.operation.Find

import org.mongodb.scala.core.{MongoCollectionOptions, MongoCollectionViewProvider}

/**
 * The MongoCollectionView used in chaining CRUD operations together
 *
 * @param client The MongoClient
 * @param namespace The MongoNamespace of the collection
 * @param codec The codec
 * @param options The MongoCollectionOptions
 * @param findOp The FindOp
 * @param writeConcern The current WriteConcern
 * @param limitSet flag indicating if `limit()` has been called
 * @param doUpsert flag indicicating `upsert()` has been called
 * @param readPreference the ReadPreference to use for this operation
 * @tparam T the collection type (usually document)
 */
protected case class MongoCollectionView[T](client: MongoClient, namespace: MongoNamespace, codec: CollectibleCodec[T],
                                            options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern,
                                            limitSet: Boolean, doUpsert: Boolean, readPreference: ReadPreference)
  extends MongoCollectionViewProvider[T] with RequiredTypesAndTransformers {

  /**
   * Create a copy of MongoCollectionView[T]
   *
   * @inheritdoc
   */
  protected def copy(client: MongoClient, namespace: MongoNamespace, codec: CollectibleCodec[T],
                     options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern, limitSet: Boolean,
                     doUpsert: Boolean, readPreference: ReadPreference): MongoCollectionView[T] = {
    MongoCollectionView[T](client, namespace, codec, options, findOp: Find, writeConcern, limitSet, doUpsert,
                           readPreference)
  }
}
