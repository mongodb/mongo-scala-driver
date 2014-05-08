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
 *
 */
package org.mongodb.scala.rxscala.admin

import java.util

import scala.collection.JavaConverters._

import org.mongodb.Document
import org.mongodb.codecs.DocumentCodec
import org.mongodb.operation.CommandReadOperation

import org.mongodb.scala.rxscala.MongoClient
import org.mongodb.scala.rxscala.utils.HandleCommandResponse

import rx.lang.scala.Observable

case class MongoClientAdmin(client: MongoClient) extends HandleCommandResponse {

  private val ADMIN_DATABASE = "admin"
  private val PING_COMMAND = new Document("ping", 1)
  private val LIST_DATABASES = new Document("listDatabases", 1)
  private val commandCodec: DocumentCodec = new DocumentCodec()

  def ping: Observable[Double] = {
    val operation = createOperation(PING_COMMAND)
    handleErrors(client.executeAsync(operation,  client.options.readPreference)) map {
      result => result.getResponse.getDouble("ok")
    }
  }

  def databaseNames: Observable[Seq[String]] = {
    val operation = createOperation(LIST_DATABASES)
    handleErrors(client.executeAsync(operation,  client.options.readPreference)) map {
      result =>  {
        val databases = result.getResponse.get("databases").asInstanceOf[util.ArrayList[Document]]
        databases.asScala.map(doc => doc.getString("name")).toSeq
      }
    }
  }

  private def createOperation(command: Document) = {
    // scalastyle:off null magic.number
    new CommandReadOperation(ADMIN_DATABASE, command, commandCodec, commandCodec)
    // scalastyle:on null magic.number
  }
}
