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
package org.mongodb.scala.rxscala.admin

import java.util

import scala.collection.JavaConverters._

import rx.lang.scala.Observable

import org.mongodb.{CommandResult, Document}

import org.mongodb.scala.core.admin.MongoClientAdminProvider
import org.mongodb.scala.rxscala.{RequiredTypes, CommandResponseHandler, MongoClient}

case class MongoClientAdmin(client: MongoClient) extends MongoClientAdminProvider
  with CommandResponseHandler with RequiredTypes {

  /**
   * A helper that takes the ping CommandResult and returns the ping response time from the "ok" field
   *
   * @return ping times
   */
  protected def pingHelper: Observable[CommandResult] => Observable[Double] = {result =>
    result map { cmdResult => cmdResult.getResponse.getDouble("ok") }
  }

  /**
   * A helper that gets the database list from the CommandResult and returns the names of the databases
   *
   * @return database names
   */
  protected def databaseNamesHelper: Observable[CommandResult] => Observable[String] = {
    result => {
      result.map({ cmdResult =>
        val databases = cmdResult.getResponse.get("databases").asInstanceOf[util.ArrayList[Document]]
        Observable.from(databases.asScala.map(doc => doc.getString("name")).toList)
      }).concat
    }
  }
}
