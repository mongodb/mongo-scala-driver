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
package org.mongodb.scala.core.admin

import scala.language.higherKinds

import org.mongodb.{CommandResult, Document}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.operation.CommandReadOperation

import org.mongodb.scala.core.{CommandResponseHandlerProvider, MongoClientProvider, RequiredTypesProvider}

trait MongoClientAdminProvider {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  val client: MongoClientProvider
  def ping: ResultType[Double]
  def databaseNames: ListResultType[String]

  private val ADMIN_DATABASE = "admin"
  private val PING_COMMAND = new Document("ping", 1)
  private val LIST_DATABASES = new Document("listDatabases", 1)
  private val commandCodec: DocumentCodec = new DocumentCodec()

  protected def pingRaw(transformer: ResultType[CommandResult] => ResultType[Double]): ResultType[Double] = {
    val operation = createOperation(PING_COMMAND)
    val result = client.executeAsync(operation,  client.options.readPreference).asInstanceOf[ResultType[CommandResult]]
    transformer(handleErrors(result))
  }

  def databaseNamesRaw(transformer: ResultType[CommandResult] => ListResultType[String]): ListResultType[String] = {
    val operation = createOperation(LIST_DATABASES)
    val result = client.executeAsync(operation,  client.options.readPreference).asInstanceOf[ResultType[CommandResult]]
    transformer(handleErrors(result))
  }

  protected def createOperation(command: Document) =
    new CommandReadOperation(ADMIN_DATABASE, command, commandCodec, commandCodec)
}
