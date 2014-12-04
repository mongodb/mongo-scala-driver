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

import com.mongodb.ReadPreference
import com.mongodb.ReadPreference.primary
import com.mongodb.assertions.Assertions.notNull
import com.mongodb.async.ErrorHandlingResultCallback.wrapCallback
import com.mongodb.async.SingleResultCallback
import com.mongodb.binding.{ AsyncClusterBinding, AsyncReadBinding, AsyncReadWriteBinding, AsyncWriteBinding }
import com.mongodb.client.options.OperationOptions
import com.mongodb.connection.Cluster
import com.mongodb.operation.{ AsyncWriteOperation, AsyncReadOperation, GetDatabaseNamesOperation, AsyncOperationExecutor }

/**
 * The MongoClientProvider trait providing the core of a MongoClient implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the
 * concrete implementation uses. The current supported implementations are native Scala `Futures` and RxScala
 * `Observables`.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation. To do this the concrete implementation of this trait requires the following
 * methods to be implemented:
 *
 * {{{
 *    case class MongoClient(options: MongoClientOptions, cluster: Cluster)
 *        extends MongoClientProvider with RequiredTypesAndTransformers
 *
 *         protected def databaseProvider(databaseName: String, databaseOptions: MongoDatabaseOptions): Database
 *
 * }}}
 *
 */
trait MongoClientProvider extends Closeable with ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * The MongoClientOptions used with MongoClient.
   * These form the basis of the default options for the database and collection.
   *
   * @note Its expected that the MongoClient implementation is a case class and this is one of the constructor params.
   *       The default MongoClientOptions is created automatically by the [[MongoClientCompanion]] helper.
   */
  val options: MongoClientOptions

  /**
   * TODO - document
   */
  val operationOptions: OperationOptions

  /**
   * The Cluster.
   *
   * Used in conjunction with the binding for an operation to target the query at the correct server.
   *
   * @note Its expected that the MongoClient implementation is a case class and this is one of the constructor params.
   *       The default Cluster is created automatically by the [[MongoClientCompanion]] helper.
   */
  val cluster: Cluster

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
  def apply(databaseName: String, databaseOptions: OperationOptions) = database(databaseName, databaseOptions)

  /**
   * An explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @return MongoDatabase
   */
  def database(databaseName: String): Database = database(databaseName, operationOptions)

  /**
   * an explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def database(databaseName: String, databaseOptions: OperationOptions): Database =
    databaseProvider(databaseName, databaseOptions.withDefaults(operationOptions))

  /**
   * Close the MongoClient and its connections
   */
  def close() {
    cluster.close()
  }

  /**
   * List the database names
   *
   * @return ListResultType[String]
   */
  def databaseNames: ListResultType[String] =
    executeAsync(new GetDatabaseNamesOperation(), primary, listToListResultTypeCallback[String]())

  /**
   * A concrete implementation of a [[MongoDatabaseProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   *
   * @return Database
   */
  protected def databaseProvider(databaseName: String, databaseOptions: OperationOptions): Database

  /**
   * TODO async operation executor
   */
  protected val executor: AsyncOperationExecutor = createOperationExecutor(options, cluster)

  protected def createOperationExecutor(options: MongoClientOptions, cluster: Cluster): AsyncOperationExecutor = {
    new AsyncOperationExecutor {

      def execute[T](operation: AsyncReadOperation[T], readPreference: ReadPreference,
                     callback: SingleResultCallback[T]) {
        val wrappedCallback: SingleResultCallback[T] = wrapCallback(callback)
        val binding: AsyncReadBinding = getReadWriteBinding(readPreference, options, cluster)
        operation.executeAsync(binding, new SingleResultCallback[T] {
          override def onResult(result: T, t: Throwable): Unit = {
            try {
              wrappedCallback.onResult(result, t)
            } finally {
              binding.release()
            }
          }
        })
      }

      def execute[T](operation: AsyncWriteOperation[T], callback: SingleResultCallback[T]) {
        val binding: AsyncWriteBinding = getReadWriteBinding(ReadPreference.primary, options, cluster)
        operation.executeAsync(binding, new SingleResultCallback[T] {
          override def onResult(result: T, t: Throwable): Unit = {
            try {
              wrapCallback(callback).onResult(result, t)
            } finally {
              binding.release()
            }
          }
        })
      }
    }
  }

  private def getReadWriteBinding(readPreference: ReadPreference, options: MongoClientOptions,
                                  cluster: Cluster): AsyncReadWriteBinding = {
    notNull("readPreference", readPreference)
    new AsyncClusterBinding(cluster, readPreference)
  }
}
