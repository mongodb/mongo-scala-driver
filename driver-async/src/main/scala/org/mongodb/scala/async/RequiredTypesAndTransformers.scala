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

import com.mongodb.client.model.FindOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.{AsyncBatchCursor, AsyncOperationExecutor, AsyncReadOperation}
import com.mongodb.{MongoNamespace, ReadPreference}
import org.mongodb.scala.core.RequiredTypesAndTransformersProvider

import scala.concurrent.Future

trait RequiredTypesAndTransformers extends RequiredTypesAndTransformersProvider {

  /* Concrete Implementations */
  type Client = MongoClient
  type Database = MongoDatabase
  type Collection[T] = MongoCollection[T]
  type FindFluent[T] = MongoCollectionFindFluent[T]
  type OperationIterable[T] = MongoOperationIterable[T]

  /* Desired Data Types */
  type ResultType[T] = Future[T]
  type ListResultType[T] = Future[List[T]]

  /* Required Helpers  */
  protected def findFluent[T](namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                              options: OperationOptions, executor: AsyncOperationExecutor,
                              clazz: Class[T]): FindFluent[T] =
    new MongoCollectionFindFluent[T](namespace, filter, findOptions, options, executor, clazz)

  protected def operationIterable[T](operation: AsyncReadOperation[AsyncBatchCursor[T]],
                                     readPreference: ReadPreference, executor: AsyncOperationExecutor,
                                     clazz: Class[T]): OperationIterable[T] =
    new MongoOperationIterable[T](operation, readPreference, executor, clazz)

  /* Transformers (Not robots in disguise but apply-to-all functions) */

  protected def listResultTypeConverter[T](): Future[List[T]] => ListResultType[T] = result => result

  protected def resultTypeConverter[T](): Future[T] => ResultType[T] = result => result
}
