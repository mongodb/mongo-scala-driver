/**
 * Copyright 2010-2014 MongoDB, Inc. <http://www.mongodb.org>
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
 * [Project URL - TODO]
 *
 */
package org.mongodb.scala.integration


import org.mongodb.Document
import org.mongodb.connection.ServerAddress

import org.mongodb.scala._
import org.mongodb.scala.helpers.RequiresMongoDBSpec

class MongoClientISpec extends RequiresMongoDBSpec {

  "MongoClient" should "be instantiatable without params" in {
    val myClient = MongoClient()
    myClient shouldBe a[MongoClient]
    myClient.options shouldBe a[MongoClientOptions]
  }

  it should "take a ServerAddress" in {
    val myClient = MongoClient(new ServerAddress("localhost:27017"))
    myClient shouldBe a[MongoClient]
    myClient.options shouldBe a[MongoClientOptions]
  }

  it should "take a ServerAddress with Options" in {
    val myClient = MongoClient(new ServerAddress(),
      MongoClientOptions(description="test"))
    myClient.options.description shouldBe "test"
  }

  it should "Take a List of servers" in {
    val myClient = MongoClient(List(new ServerAddress("localhost:27017")))
    myClient shouldBe a[MongoClient]
    myClient.options shouldBe a[MongoClientOptions]
  }

  it should "take a mongo uri" in {
    val myClient = MongoClient(MongoClientURI("mongodb://localhost"))
    myClient shouldBe a[MongoClient]
    myClient.options shouldBe a[MongoClientOptions]
  }

  it should "Be able to get a database" in {
    val myClient = MongoClient()
    val database = myClient("test")
    database shouldBe a[MongoDatabase]
    database.name shouldBe "test"
  }

  it should "Be able to get a database via apply" in {
    val myClient = MongoClient()
    val database = myClient("test")
    database shouldBe a[MongoDatabase]
    database.name shouldBe "test"
  }

  it should "Be able to get a collection" in {
    val database = MongoClient()("mongoScalaTest")
    val collection = database.collection("test")
    collection shouldBe a[MongoCollection[Document]]
    collection.name shouldBe "test"
  }

  it should "Be able to get a collection via apply" in {
    val database = MongoClient()("mongoScalaTest")
    val collection = database("test")
    collection shouldBe a[MongoCollection[Document]]
    collection.name shouldBe "test"
  }

}
