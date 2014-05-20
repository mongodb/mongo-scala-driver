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
package org.mongodb.scala.core

import java.io.Closeable
import java.util.concurrent.TimeUnit

import scala.language.higherKinds

import org.mongodb.ReadPreference
import org.mongodb.binding.{AsyncClusterBinding, AsyncReadBinding, AsyncReadWriteBinding, AsyncWriteBinding}
import org.mongodb.connection.{BufferProvider, Cluster}
import org.mongodb.operation.{QueryOperation, AsyncReadOperation, AsyncWriteOperation}
import org.mongodb.scala.core.admin.MongoClientAdminProvider

trait MongoClientProvider extends Closeable {

  this: RequiredTypesProvider =>

  val options: MongoClientOptions
  val cluster: Cluster
  val bufferProvider: BufferProvider

  /**
   * Helper to get a database
   *
   * @param databaseName the name of the database
   * @return MongoDatabase
   */
  def apply(databaseName: String) = database(databaseName)

  /**
   * Helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def apply(databaseName: String, databaseOptions: MongoDatabaseOptions) = database(databaseName, databaseOptions)

  /**
   * An explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @return MongoDatabase
   */
  def database(databaseName: String): Database = database(databaseName, MongoDatabaseOptions(options))

  /**
   * an explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def database(databaseName: String, databaseOptions: MongoDatabaseOptions): Database

  /**
   * Close the MongoClient and its connections
   */
  def close() {
    cluster.close()
  }

  /**
   * The MongoClientAdmin which provides admin methods
   */
  val admin: MongoClientAdminProvider

  private[scala] def executeAsync[T](writeOperation: AsyncWriteOperation[T]): ResultType[T]

  private[scala] def executeAsync[T](queryOperation: QueryOperation[T], readPreference: ReadPreference): CursorType[T]

  private[scala] def executeAsync[T](readOperation: AsyncReadOperation[T], readPreference: ReadPreference): ResultType[T]

  private[scala] def writeBinding: AsyncWriteBinding = {
    readWriteBinding(ReadPreference.primary)
  }

  private[scala] def readBinding(readPreference: ReadPreference): AsyncReadBinding = {
    readWriteBinding(readPreference)
  }

  private[scala] def readWriteBinding(readPreference: ReadPreference): AsyncReadWriteBinding = {
    new AsyncClusterBinding(cluster, readPreference, options.maxWaitTime, TimeUnit.MILLISECONDS)
  }
}
