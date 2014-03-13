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
 * [Project URL - TODO]
 *
 */

package org.mongodb.scala.integration.admin

import org.mongodb.{CreateCollectionOptions, Document, Index}

import org.mongodb.scala.admin.MongoCollectionAdmin
import org.mongodb.scala.helpers.RequiresMongoDBSpec

class MongoCollectionAdminISpec extends RequiresMongoDBSpec {

  "MongoCollectionAdmin" should "be accessible via collection.admin" in withCollection {
    collection =>
      collection.admin shouldBe a[MongoCollectionAdmin[Document]]
  }

  it should "tell if a collection is capped for non-existent collection" in withDatabase {
    database =>
      database("collection").admin.isCapped
  }

  it should "tell if a collection is capped or ordinary collection" in withDatabase {
    database =>
      database.admin.createCollection("collection").futureValue
      database("collection").admin.isCapped.futureValue shouldBe false
  }

  it should "tell if a collection is capped for a capped collection" in withDatabase {
    database =>
      val collectionOptions = new CreateCollectionOptions("testCapped", true, 256)
      database.admin.createCollection(collectionOptions).futureValue
      database("testCapped").admin.isCapped.futureValue shouldBe true
  }

  it should "get statistics" in withDatabase {
    database =>
      database.admin.createCollection("test").futureValue
      val collection = database("test")
      whenReady(collection.admin.statistics) {
        collStats =>
          collStats.get("ns") should equal(collection.namespace.getFullName)
      }
  }

  it should "get statistics for non-existent collection" in withCollection {
    collection =>
      pending
      collection.admin.statistics.futureValue.getDouble("ok") should equal(0.0)
  }

  it should "get indexes for non-existent collection" in withCollection {
    collection =>
      collection.admin.getIndexes.futureValue shouldBe empty
  }

  it should "get indexes for a new collection" in withCollection {
    collection =>
      collection.database.admin.createCollection(collection.name).futureValue
      collection.admin.getIndexes.futureValue.length should equal(1)
  }

  it should "add index" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).futureValue
      collection.admin.getIndexes.futureValue.length should equal(2)
  }

  it should "add index must throw an error for bad indexes" in withCollection {
    collection =>
      pending
      collection.admin.createIndex(Index.builder().name("test").build).futureValue
  }

  it should "drop index for non-existent collection" in withCollection {
    collection =>

      whenReady(collection.admin.dropIndex(Index.builder().addKeys("test").build)) {
        result =>
          result.getDouble("ok") should equal(0.0)
          result.getString("errmsg") should equal("ns not found")
      }

      whenReady(collection.admin.dropIndex("test_1")) {
        result =>
          result.getDouble("ok") should equal(0.0)
          result.getString("errmsg") should equal("ns not found")
      }
  }

  it should "drop index" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).futureValue
      collection.admin.dropIndex(Index.builder().addKeys("test").build).futureValue.getDouble("ok") should equal(1.0)


      collection.admin.createIndex(Index.builder().addKeys("test").build).futureValue
      collection.admin.dropIndex("test_1").futureValue.getDouble("ok") should equal(1.0)

  }

  it should "drop indexes for non-existent collection" in withCollection {
    collection =>
      whenReady(collection.admin.dropIndexes()) {
        result =>
          result.getDouble("ok") should equal(0.0)
          result.getString("errmsg") should equal("ns not found")
      }
  }

  it should "drop indexes new collection" in withCollection {
    collection =>
      whenReady(collection.database.admin.createCollection(collection.name)) {
        f =>
          collection.admin.dropIndexes().futureValue.getDouble("ok") should equal(1.0)
      }
  }

  it should "drop indexes existing collection" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).futureValue
      collection.admin.dropIndexes().futureValue.getDouble("ok") should equal(1.0)
  }

}
