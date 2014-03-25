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
package org.mongodb.scala.integration.admin

import scala.concurrent._

import org.mongodb.{Document, MongoCredential}
import org.mongodb.scala.admin.MongoDatabaseAdmin
import org.mongodb.scala.helpers.RequiresMongoDBSpec

class MongoDatabaseAdminISpec extends RequiresMongoDBSpec {

  "MongoDatabaseAdmin" should "be accessible via mongoDatabase.admin" in withDatabase {
    database =>
      val admin = database.admin
      admin shouldBe a[MongoDatabaseAdmin]
  }

  it should "remove all collections once drop() is called" in withDatabase {
    database =>
      database.admin.createCollection("test").futureValue.isOk shouldBe true
      database.admin.drop().futureValue.isOk shouldBe true
      database.admin.collectionNames.futureValue should equal(List.empty)
  }

  it should "return collectionNames" in withDatabase {
    database =>
      database.admin.createCollection("checkNames").futureValue.isOk shouldBe true
      database.admin.collectionNames.futureValue should contain theSameElementsAs List("checkNames", "system.indexes")
  }

  it should "create collection" in withDatabase {
    database =>
      database.admin.createCollection("test").futureValue.isOk shouldBe true
      database.admin.collectionNames.futureValue should contain theSameElementsAs List("test", "system.indexes")
  }

  it should "rename collection" in withDatabase {
    database =>
      database.admin.createCollection("test").futureValue.isOk shouldBe true
      database.admin.collectionNames.futureValue should contain theSameElementsAs List("test", "system.indexes")
      database.admin.renameCollection("test", "new").futureValue.isOk shouldBe true
      database.admin.collectionNames.futureValue should contain theSameElementsAs List("new", "system.indexes")
  }

  it should "allow you to add a user then authenticate" in {
    pending
    val credential: MongoCredential = MongoCredential.createMongoCRCredential("xx", "test", "e".toCharArray)
    // MongoClient()("mongoScalaTest").admin.addUser(credential.getUserName(), credential.getPassword(), true)

    // Todo confirm this works
    // MongoClient(List(credential))("test")("test").one()
    // RemoveUser
    // Close connection
  }
}
