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
package org.mongodb.scala.async.integration.admin

import scala.concurrent.Future

import org.mongodb.scala.async.MongoClient
import org.mongodb.scala.async.admin.MongoClientAdmin
import org.mongodb.scala.async.helpers.RequiresMongoDBSpec

class MongoClientAdminISpec extends RequiresMongoDBSpec {

  "MongoClientAdmin" should "be accessible via MongoClient().admin" in {
    val admin = MongoClient().admin
    admin shouldBe a[MongoClientAdmin]
  }

  it should "Return a ping" in {
    checkMongoDB()
    val ping = mongoClient.admin.ping
    ping shouldBe a[Future[Double]]
    ping.futureValue === 1.0
  }

  it should "Return an IllegalStateException exception on a closed MongoClient" in {
    val client = MongoClient()
    client.close()
    intercept[IllegalStateException] {
      client.admin.ping
    }
  }

  it should "Return a seq of database names" in {
    checkMongoDB()
    val dbNames = mongoClient.admin.databaseNames
    dbNames shouldBe a[Future[Seq[String]]]
  }

}
