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

import scala.Some

import rx.lang.scala.{Observable, Subscriber}

import org.mongodb.{Block, MongoAsyncCursor, MongoException, MongoFuture}
import org.mongodb.binding.ReferenceCounted
import org.mongodb.connection.{Cluster, SingleResultCallback}

import org.mongodb.scala.core.{MongoClientCompanion, MongoClientOptions, MongoClientProvider, MongoDatabaseOptions}
import org.mongodb.scala.rxscala.admin.MongoClientAdmin

/**
 * A factory for creating a [[MongoClient]] instance.
 */
object MongoClient extends MongoClientCompanion with RequiredTypes

/**
 * The MongoClient
 *
 * Normally created via the companion object helpers
 *
 * @param options The connection options
 * @param cluster The underlying cluster
 */
case class MongoClient(options: MongoClientOptions, cluster: Cluster)
  extends MongoClientProvider with RequiredTypes {

  /**
   * Provides the MongoClientAdmin for this MongoClient
   *
   * @return MongoClientAdmin
   */
  val admin: MongoClientAdmin = MongoClientAdmin(this)

  /**
   * an explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  protected def databaseProvider(databaseName: String, databaseOptions: MongoDatabaseOptions) =
    MongoDatabase(databaseName, this, databaseOptions)

  /**
   * A type converter method that converts a `MongoFuture[T]` to a rxScala `Observable[T]`
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
   * A type converter method that converts a `MongoFuture[MongoAsyncCursor[T]]` to a rxScala `Observable[T]`
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
}
