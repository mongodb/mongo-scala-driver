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

import org.mongodb.scala.core.RequiredTypesAndTransformersProvider

import rx.lang.scala.{Subscriber, Observable}
import org.mongodb.{Block, MongoException, MongoAsyncCursor, MongoFuture}
import org.mongodb.binding.ReferenceCounted
import org.mongodb.connection.SingleResultCallback

trait RequiredTypesAndTransformers extends RequiredTypesAndTransformersProvider {

  /* Concrete Implementations */

  type Client = MongoClient
  type Database = MongoDatabase
  type Collection[T] = MongoCollection[T]
  type CollectionView[T] = MongoCollectionView[T]

  /* Desired Data Types */
  type ResultType[T] = Observable[T]
  type ListResultType[T] = Observable[T]
  type CursorType[T] = Observable[T]

  /* Transformers (Not robots in disguise but apply-to-all functions) */

  /**
   * A type converter method that converts a `MongoFuture[MongoAsyncCursor[T]]` to `Observable[T]`
   *
   * @tparam T the type of result eg CommandResult, Document etc..
   * @return CursorType[T]
   */
  protected def mongoCursorConverter[T]: (MongoFuture[MongoAsyncCursor[T]], ReferenceCounted) => Observable[T] = {
    (result, binding) => {
      Observable((subscriber: Subscriber[T]) => {
        result.register(new SingleResultCallback[MongoAsyncCursor[T]] {
          override def onResult(cursor: MongoAsyncCursor[T], e: MongoException): Unit = {
            Option(e) match {
              case Some(err) => subscriber.onError(err)
              case None =>
                cursor.forEach(new Block[T] {
                  override def apply(t: T): Unit = {
                    if (subscriber.isUnsubscribed) {
                      subscriber.onCompleted()
                    } else {
                      subscriber.onNext(t)
                    }
                  }
                }).register(new SingleResultCallback[Void] {
                  def onResult(result: Void, e: MongoException) {
                    binding.release()
                    if (e != null) subscriber.onError(e)
                    subscriber.onCompleted()
                  }
                })
            }
          }
        })
      })
    }
  }

  /**
   * A type converter method that converts a `MongoFuture[T]` to `Observable[T]`
   *
   * @tparam T the type of result eg CommandResult, Document etc..
   * @return Observable[T]
   */
  protected def mongoFutureConverter[T]: (MongoFuture[T], ReferenceCounted) => Observable[T] = {
    (result, binding) => {
      Observable((subscriber: Subscriber[T]) => {
        result.register(new SingleResultCallback[T] {
          override def onResult(result: T, e: MongoException): Unit = {
            try {
              Option(e) match {
                case Some(err) => subscriber.onError(err)
                case None => subscriber.onNext(result)
              }
            }
            finally {
              binding.release()
              subscriber.onCompleted()
            }
          }
        })
      })
    }
  }

  /**
   * A type transformer that takes a `ResultType[Void]` and converts it to `ResultType[Unit]`
   *
   * This is needed as returning `Void` is not idiomatic in scala whereas a `Unit` is more acceptable.
   *
   * @return ResultType[Unit]
   */
  protected def voidToUnitConverter: Observable[Void] => Observable[Unit] = result => result map { v => Unit }

  /**
   * A type transformer that converts a `Observable[List[T]]` to `Observable[T]`
   *
   * @tparam T List data type of item eg Document or String
   * @return the correct ListResultType[T]
   */
  protected def listToListResultTypeConverter[T]: Observable[List[T]] => Observable[T] = { result =>
    result.map({ items => Observable.from(items) }).concat
  }
}
