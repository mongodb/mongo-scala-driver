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

/**
 * The MongoClientAdmin trait providing the core of a MongoClientAdmin implementation.
 *
 * To use the trait it requires a concrete implementation of [CommandResponseHandlerProvider] and
 * [RequiredTypesProvider] to define handling of CommandResult errors and the types the concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesProvider] implementation. To do this the concrete implementation of this trait requires the following
 * methods to be implemented:
 *
 * {{{
 *    case class MongoClientAdmin(client: MongoClient) extends MongoClientAdminProvider with
 *      CommandResponseHandler {
 *
 *        protected def pingHelper: ResultType[CommandResult] => ResultType[Double]
 *
 *        protected def databaseNamesHelper:ResultType[CommandResult] => ListResultType[String]
 *    }
 * }}}
 */
trait MongoClientAdminProvider {

  this: CommandResponseHandlerProvider with RequiredTypesProvider =>

  /**
   * The MongoClient we are administrating
   */
  val client: MongoClientProvider

  /**
   * The ping time to the MongoDB Server
   *
   * @return ResultType[Double]
   */
  def ping: ResultType[Double] = {
    val operation = createOperation(PING_COMMAND)
    val result = client.executeAsync(operation,  client.options.readPreference).asInstanceOf[ResultType[CommandResult]]
    pingHelper(handleErrors(result))
  }

  /**
   * List the database names
   *
   * @return ListResultType[String]
   */
  def databaseNames: ListResultType[String] = {
    val operation = createOperation(LIST_DATABASES)
    val result = client.executeAsync(operation,  client.options.readPreference).asInstanceOf[ResultType[CommandResult]]
    databaseNamesHelper(handleErrors(result)).asInstanceOf[ListResultType[String]]
  }

  /**
   * A helper that takes the ping CommandResult and returns the ping response time from the "ok" field
   *
   * An example for Futures would be:
   * {{{
   *   result => result map { cmdResult => cmdResult.getResponse.getDouble("ok") }
   * }}}
   *
   * @return the ping time
   */
  protected def pingHelper: ResultType[CommandResult] => ResultType[Double]

  /**
   * A helper that gets the database list from the CommandResult and returns the names of the databases
   *
   * An example for Futures would be:
   * {{{
   *    result =>
   *       result map {
   *         cmdResult => {
   *           val databases = cmdResult.getResponse.get("databases").asInstanceOf[util.ArrayList[Document]]
   *           databases.asScala.map(doc => doc.getString("name")).toList
   *         }
   *       }
   * }}}
   *
   * @return The database names
   */
  protected def databaseNamesHelper: ResultType[CommandResult] => ListResultType[String]

  private val ADMIN_DATABASE = "admin"
  private val PING_COMMAND = new Document("ping", 1)
  private val LIST_DATABASES = new Document("listDatabases", 1)
  private val commandCodec: DocumentCodec = new DocumentCodec()
  private def createOperation(command: Document) = {
    new CommandReadOperation(ADMIN_DATABASE, command, commandCodec, commandCodec)
  }
}
