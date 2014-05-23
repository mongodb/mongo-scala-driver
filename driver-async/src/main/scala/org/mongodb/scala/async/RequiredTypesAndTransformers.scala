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

import scala.concurrent.{Future, Promise}

import org.mongodb.{MongoAsyncCursor, MongoException, MongoFuture}
import org.mongodb.binding.ReferenceCounted
import org.mongodb.connection.SingleResultCallback

import org.mongodb.scala.core.RequiredTypesAndTransformersProvider

trait RequiredTypesAndTransformers extends RequiredTypesAndTransformersProvider {

  /* Concrete Implementations */
  type Client = MongoClient
  type Database = MongoDatabase
  type Collection[T] = MongoCollection[T]
  type CollectionView[T] = MongoCollectionView[T]

  /* Desired Data Types */
  type ResultType[T] = Future[T]
  type ListResultType[T] = Future[List[T]]
  type CursorType[T] = Future[MongoAsyncCursor[T]]

  /* Transformers (Not robots in disguise but apply-to-all functions) */

  /**
   * A type converter method that converts a `MongoFuture` to a of `Future[T]`
   */
  protected def mongoFutureConverter[T]: (MongoFuture[T], ReferenceCounted) => Future[T] = {
    (result, binding) => {
      val promise = Promise[T]()
      result.register(new SingleResultCallback[T] {
        override def onResult(result: T, e: MongoException): Unit = {
          try {
            Option(e) match {
              case None => promise.success(result)
              case _ => promise.failure(e)

            }
          }
          finally {
            binding.release()
          }
        }
      })
      promise.future
    }
  }

  /**
   * A type converter method that converts a `MongoFuture[MongoAsyncCursor[T]]` to `Future[MongoAsyncCursor[T]]`
   */
  protected def mongoCursorConverter[T]: (MongoFuture[MongoAsyncCursor[T]], ReferenceCounted) => Future[MongoAsyncCursor[T]] = {
    (result, binding) =>
      val promise = Promise[MongoAsyncCursor[T]]()

      result.register(new SingleResultCallback[MongoAsyncCursor[T]] {
        override def onResult(result: MongoAsyncCursor[T], e: MongoException): Unit = {
          try {
            Option(e) match {
              case None => promise.success(result)
              case _ => promise.failure(e)

            }
          }
          finally {
            binding.release()
          }
        }
      })
      promise.future
  }

  /**
   * A type transformer that converts a `Future[List[T\]\]` to `Future[List[T\]\]`
   *
   * Nothing needed for `Futures`
   *
   * @tparam T List data type of item eg Document or String
   * @return the future list
   */
  protected def listToListResultTypeConverter[T]: Future[List[T]] => Future[List[T]] = result => result

  /**
   * A type transformer that takes a `Future[Void]` and converts it to `Future[Unit]`
   *
   * @return Future[Unit]
   */
  protected def voidToUnitConverter: Future[Void] => Future[Unit] = result => result.mapTo[Unit]

}
