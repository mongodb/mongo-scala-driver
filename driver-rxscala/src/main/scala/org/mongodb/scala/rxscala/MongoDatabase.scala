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

import com.mongodb.MongoNamespace
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.AsyncOperationExecutor
import org.mongodb.scala.core.MongoDatabaseProvider

case class MongoDatabase(name: String, options: OperationOptions, executor: AsyncOperationExecutor)
  extends MongoDatabaseProvider with RequiredTypesAndTransformers {
  /**
   * A concrete implementation of [[org.mongodb.scala.core.MongoCollectionProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   *
   * @param collectionName the name of the collection
   * @param operationOptions the options to use with the collection
   * @param executor the AsyncOperationExecutor to be used with this MongoDatabase instance
   * @param clazz the document return class type
   * @tparam T the document type
   * @return the collection
   */
  override def collection[T](collectionName: String, operationOptions: OperationOptions,
                             executor: AsyncOperationExecutor, clazz: Class[T]): Collection[T] = {
    new MongoCollection[T](new MongoNamespace(name, collectionName), operationOptions, executor, clazz)
  }
}