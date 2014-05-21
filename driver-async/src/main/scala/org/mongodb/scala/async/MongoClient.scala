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

import org.mongodb.{MongoFuture, MongoAsyncCursor, MongoException, ReadPreference}
import org.mongodb.connection.{BufferProvider, Cluster, SingleResultCallback}
import org.mongodb.operation.{QueryOperation, AsyncReadOperation, AsyncWriteOperation}

import org.mongodb.scala.core._
import org.mongodb.scala.async.admin.MongoClientAdmin
import org.mongodb.binding.ReferenceCounted

/**
 * A factory for creating a [[org.mongodb.scala.async.MongoClient MongoClient]] instance.
 */
object MongoClient extends MongoClientCompanion with RequiredTypes

/**
 * The MongoClient
 *
 * Normally created via the companion object helpers
 *
 * @param options The connection options
 * @param cluster The underlying cluster
 * @param bufferProvider The buffer provider to use
 */
case class MongoClient(options: MongoClientOptions, cluster: Cluster, bufferProvider: BufferProvider)
  extends MongoClientProvider with RequiredTypes {

  val admin = MongoClientAdmin(this)

  /**
   * an explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def database(databaseName: String, databaseOptions: MongoDatabaseOptions) =
    MongoDatabase(databaseName, this, databaseOptions)

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
}
