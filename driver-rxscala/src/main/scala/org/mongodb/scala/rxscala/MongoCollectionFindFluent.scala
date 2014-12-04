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
import com.mongodb.client.model.FindOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.AsyncOperationExecutor
import org.mongodb.scala.core.MongoCollectionFindFluentProvider

/**
 *
 * @param namespace
 * @param filter
 * @param findOptions
 * @param options
 * @param executor
 * @param clazz
 * @tparam T
 */
case class MongoCollectionFindFluent[T](namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                                        options: OperationOptions, executor: AsyncOperationExecutor, clazz: Class[T])
  extends MongoCollectionFindFluentProvider[T] with RequiredTypesAndTransformers {

  /**
   * A copy method to produce a new updated version of a `FindFluent`
   *
   * @param filter the filter, which may be null.
   * @param findOptions the options to apply to a find operation (also commonly referred to as a query).
   * @param options the OperationOptions for the find
   * @param executor the execution handler for the find operation
   * @param clazz the resulting class type
   * @return
   */
  override protected def copy(namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                              options: OperationOptions, executor: AsyncOperationExecutor,
                              clazz: Class[T]): FindFluent[T] =
    MongoCollectionFindFluent(namespace, filter, findOptions, options, executor, clazz)
}
