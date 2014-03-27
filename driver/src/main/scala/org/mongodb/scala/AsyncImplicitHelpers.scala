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
package org.mongodb.scala

import scala.concurrent._

import rx.lang.scala._

import org.mongodb.{MongoAsyncCursor, MongoException}
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation._

/**
 * Async  to implicitly wrap QueryOperations to add extra methods to allow iteration of future cursor results
 */
object AsyncImplicitHelpers {

  /**
   * Implicit QueryOperation Extension that converts the operation into a reactive
   * `Subject` allowing a stream of documents an processed without blocking
   *
   * @param operation the query operation that to return a reactive cursor
   * @tparam T the Type of the resulting documents
   */
  implicit class OperationObservableCursor[T](val operation: QueryOperation[T]) {
    def cursor: Subject[T] = {
      val block = AsyncBlock[T]()
      val futureOp = operation.executeAsync
      futureOp.register(new SingleResultCallback[MongoAsyncCursor[T]] {
        def onResult(cursor: MongoAsyncCursor[T], e: MongoException) {
          if (Option(e) != None) throw e
          cursor.start(block)
        }
      })
      block.subject
    }
  }

  /**
   * Implicit Observable Extension that converts an `Observable[T]` into a `Future[T]`
   *
   * @throws IllegalArgumentException if the observer is empty or has more than one item in it
   * @param obs the observable to convert into a future
   * @tparam T The type of Observer
   */
  implicit class ObservableExtensions[+T](val obs: Observable[T]) {

    /**
     * Gets a single item from an Observable.
     *
     * Checks to make sure that the observable only has a single item in it.
     *
     * @throws IllegalArgumentException if 0 or more than 1 item in the observable
     * @return Observable.head
     */
    def single: Observable[T] =
      obs.toSeq.map(seq => seq.size match {
        case 0 => throw new IllegalArgumentException("single on empty Observable")
        case 1 => seq.head
        case _ => throw new IllegalArgumentException("single called on Observable with several items")
      })

    /**
     * Converts an `Observable[T]` into a `Future[T]`
     * @return Future[T]
     */
    def toFuture: Future[T] = {
      val p = Promise[T]()
      obs.single.subscribe(v => p.success(v), err => p.failure(err))
      p.future
    }
  }

}
