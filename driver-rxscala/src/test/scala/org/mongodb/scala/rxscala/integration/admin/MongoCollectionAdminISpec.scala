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
package org.mongodb.scala.rxscala.integration.admin

import org.mongodb.{CreateCollectionOptions, Document, Index}

import org.mongodb.scala.rxscala.admin.MongoCollectionAdmin
import org.mongodb.scala.rxscala.helpers.RequiresMongoDBSpec

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
      database.admin.createCollection("collection").observableValue
      database("collection").admin.isCapped.observableValue shouldBe false
  }

  it should "tell if a collection is capped for a capped collection" in withDatabase {
    database =>
      val collectionOptions = new CreateCollectionOptions("testCapped", true, 256)
      database.admin.createCollection(collectionOptions).observableValue
      database("testCapped").admin.isCapped.observableValue shouldBe true
  }

  it should "get statistics" in withDatabase {
    database =>
      database.admin.createCollection("test").observableValue
      val collection = database("test")
      val collStats = collection.admin.statistics.map { d => d.get("ns") }
      collStats.observableValue should equal(collection.namespace.getFullName)
  }

  it should "get statistics for non-existent collection" in withCollection {
    collection =>
      collection.admin.statistics.observableValue.getDouble("ok") should equal(0.0)
  }

  it should "get indexes for non-existent collection" in withCollection {
    collection =>
      collection.admin.getIndexes.observableValue shouldBe empty
  }

  it should "get indexes for a new collection" in withCollection {
    collection =>
      collection.database.admin.createCollection(collection.name).observableValue
      collection.admin.getIndexes.observableValue.length should equal(1)
  }

  it should "add index" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).observableValue
      collection.admin.getIndexes.observableValue.length should equal(2)
  }

  it should "add index must throw an error for bad indexes" in withCollection {
    collection =>
      pending
      collection.admin.createIndex(Index.builder().build).observableValue
  }

  it should "drop index for non-existent collection" in withCollection {
    collection =>
      collection.admin.dropIndex(Index.builder().addKeys("test").build).observableValue
      collection.admin.dropIndex("test_1").observableValue
  }

  it should "drop index" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).observableValue
      collection.admin.dropIndex(Index.builder().addKeys("test").build).observableValue

      collection.admin.createIndex(Index.builder().addKeys("test").build).observableValue
      collection.admin.dropIndex("test_1").observableValue
  }

  it should "drop indexes for non-existent collection" in withCollection {
    collection =>
      collection.admin.dropIndexes().observableValue
  }

  it should "drop indexes new collection" in withCollection {
    collection =>
      collection.database.admin.createCollection(collection.name).observableValue
      collection.admin.dropIndexes().observableValue
  }

  it should "drop indexes existing collection" in withCollection {
    collection =>
      collection.admin.createIndex(Index.builder().addKeys("test").build).observableValue
      collection.admin.dropIndexes().observableValue
  }

}
