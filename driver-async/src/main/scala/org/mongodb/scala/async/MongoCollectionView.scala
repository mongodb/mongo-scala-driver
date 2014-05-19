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
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import org.mongodb.{Block, CollectibleCodec, MongoAsyncCursor, MongoException, MongoNamespace, ReadPreference, WriteConcern}
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.Find

import org.mongodb.scala.core.{MongoCollectionOptions, MongoCollectionViewProvider}

protected case class MongoCollectionView[T](client: MongoClient, namespace: MongoNamespace, codec: CollectibleCodec[T],
                                            options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern,
                                            limitSet: Boolean, doUpsert: Boolean, readPreference: ReadPreference)
  extends MongoCollectionViewProvider[T] with RequiredTypes {

  override protected def copy(client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T], options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean, readPreference: ReadPreference): MongoCollectionViewProvider[T] = {
    MongoCollectionView[T](client.asInstanceOf[MongoClient], namespace, codec, options, findOp: Find, writeConcern, limitSet, doUpsert, readPreference)
  }

  /**
   * Return a list of results (memory hungry)
   */
  def toList(): Future[List[T]] = {
    val promise = Promise[List[T]]()
    var list = List[T]()
    val futureCursor: Future[MongoAsyncCursor[T]] = cursor().asInstanceOf[Future[MongoAsyncCursor[T]]]
    futureCursor.onComplete({
      case Success(cursor) =>
        cursor.forEach(new Block[T] {
          override def apply(d: T): Unit = {
            list ::= d
          }
        }).register(new SingleResultCallback[Void] {
          def onResult(result: Void, e: MongoException) {
            if (e != null) promise.failure(e)
            else promise.success(list.reverse)
          }
        })
      case Failure(e) => promise.failure(e)
    })
    promise.future
  }

  def one(): Future[Option[T]] = {
    val promise = Promise[Option[T]]()
    val futureCursor: Future[MongoAsyncCursor[T]] = limit(1).cursor().asInstanceOf[Future[MongoAsyncCursor[T]]]
    futureCursor.onComplete({
      case Success(cursor) =>
        cursor.forEach(new Block[T] {
          override def apply(d: T): Unit = {
            if (!promise.future.isCompleted) {
              promise.success(Some(d))
            }
          }
        }).register(new SingleResultCallback[Void] {
          def onResult(result: Void, e: MongoException) {
            if (!promise.future.isCompleted) {
              promise.success(None)
            }
          }
        })
      case Failure(e) => promise.failure(e)
    })
    promise.future
  }
}
