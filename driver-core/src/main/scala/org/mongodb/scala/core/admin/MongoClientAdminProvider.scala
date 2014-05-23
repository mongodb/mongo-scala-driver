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

import java.lang.{Double => JDouble}
import scala.collection.JavaConverters._
import org.mongodb.{MongoFuture, MongoException, CommandResult, Document}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.operation.{SingleResultFuture, CommandReadOperation}

import org.mongodb.scala.core.{CommandResponseHandlerProvider, MongoClientProvider, RequiredTypesProvider}
import org.mongodb.connection.SingleResultCallback
import java.util

/**
 * The MongoClientAdmin trait providing the core of a MongoClientAdmin implementation.
 *
 * To use the trait it requires a concrete implementation of [CommandResponseHandlerProvider] and
 * [RequiredTypesProvider] to define handling of CommandResult errors and the types the concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesProvider] implementation.
 *
 * {{{
 *    case class MongoClientAdmin(client: MongoClient) extends MongoClientAdminProvider with
 *      CommandResponseHandler with RequiredTypes
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
    val transformer = { result: MongoFuture[CommandResult] =>
    // Use native Java type to avoid Scala implicit conversion of null error if there's an exception
    val future: SingleResultFuture[JDouble] = new SingleResultFuture[JDouble]
      result.register(new SingleResultCallback[CommandResult] {
        def onResult(result: CommandResult, e: MongoException): Unit = {
          Option(e) match {
            case None => future.init(result.getResponse.getDouble("ok"), null)
            case _ => future.init(null, e)
          }
        }
      })
      future
    }
    client.executeAsync(operation, client.options.readPreference, transformer).asInstanceOf[ResultType[Double]]
  }

  /**
   * List the database names
   *
   * @return ListResultType[String]
   */
  def databaseNames: ListResultType[String] = {
    val operation = createOperation(LIST_DATABASES)
    val transformer = { result: MongoFuture[CommandResult] =>
      val future: SingleResultFuture[List[String]] = new SingleResultFuture[List[String]]
      result.register(new SingleResultCallback[CommandResult] {
        def onResult(result: CommandResult, e: MongoException): Unit = {
          Option(e) match {
            case None =>
              val databases = result.getResponse.get("databases").asInstanceOf[util.ArrayList[Document]]
              future.init(databases.asScala.map(doc => doc.getString("name")).toList, null)
            case _ => future.init(null, e)
          }
        }
      })
      future
    }
    val results = client.executeAsync(operation,  client.options.readPreference, transformer)
    listToListResultTypeConverter[String](results.asInstanceOf[ResultType[List[String]]])
  }

  private val ADMIN_DATABASE = "admin"
  private val PING_COMMAND = new Document("ping", 1)
  private val LIST_DATABASES = new Document("listDatabases", 1)
  private val commandCodec: DocumentCodec = new DocumentCodec()
  private def createOperation(command: Document) = {
    new CommandReadOperation(ADMIN_DATABASE, command, commandCodec, commandCodec)
  }
}
