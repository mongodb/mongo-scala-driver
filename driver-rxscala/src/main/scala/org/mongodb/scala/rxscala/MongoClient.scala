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

import org.mongodb.connection.Cluster

import org.mongodb.scala.core.{MongoClientCompanion, MongoClientOptions, MongoClientProvider, MongoDatabaseOptions}
import org.mongodb.scala.rxscala.admin.MongoClientAdmin

/**
 * A factory for creating a [[MongoClient]] instance.
 */
object MongoClient extends MongoClientCompanion with RequiredTypesAndTransformers

/**
 * The MongoClient
 *
 * Normally created via the companion object helpers
 *
 * @param options The connection options
 * @param cluster The underlying cluster
 */
case class MongoClient(options: MongoClientOptions, cluster: Cluster) extends MongoClientProvider
  with RequiredTypesAndTransformers {

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
}
