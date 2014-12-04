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

import com.mongodb.client.options.OperationOptions
import com.mongodb.connection.Cluster
import org.mongodb.scala.core.{MongoClientCompanion, MongoClientOptions, MongoClientProvider}

/**
 * A factory for creating a [[MongoClient]] instance.
 */
object MongoClient extends MongoClientCompanion with RequiredTypesAndTransformers

/**
 * The MongoClient
 *
 * Normally created via the companion object helpers
 *
 * @inheritdoc
 * @param options The connection options
 * @param cluster The underlying cluster
 * @param operationOptions The operation options
 */
case class MongoClient(options: MongoClientOptions, cluster: Cluster, operationOptions: OperationOptions)
  extends MongoClientProvider with RequiredTypesAndTransformers {

  /**
   * A concrete implementation of a [[org.mongodb.scala.core.MongoDatabaseProvider]]
   *
   * @note Each MongoClient implementation must provide this.
   *
   * @return Database
   */
  override protected def databaseProvider(databaseName: String, databaseOptions: OperationOptions): Database =
    MongoDatabase(databaseName, databaseOptions, executor)

}
