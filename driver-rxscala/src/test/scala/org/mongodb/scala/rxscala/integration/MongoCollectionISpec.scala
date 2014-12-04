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
package org.mongodb.scala.rxscala.integration

import com.mongodb.WriteConcernResult
import org.bson.Document

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

import org.mongodb.scala.rxscala._
import org.mongodb.scala.rxscala.helpers.RequiresMongoDBSpec

import rx.lang.scala.Observable

class MongoCollectionISpec extends RequiresMongoDBSpec {

  lazy val duration = Duration("5 sec")

  "MongoCollection" should "be callable via apply" in withCollection {
    collection =>
      collection shouldBe a[MongoCollection[_]]
  }

  it should "be able to get a count" in withCollection {
    collection =>
      collection.count().observableValue should equal(0)
  }

  it should "be able to insert a document" in withCollection {
    collection =>
      collection.dropCollection().observableValue
      collection.insertOne(new Document("Hello", "World")).observableValue
      collection.count().observableValue should equal(1)
  }

  it should "into should add all items into a target" in withCollection {
    collection =>
      collection.insertMany(createDocuments(100)).observableValue
      val documents = new ArrayBuffer[Document]()
      collection.find().into(documents).observableValue
      documents.length should equal(100)
  }

  it should "allow scala like handling for filtered collections" in withCollection {
    collection =>
      collection.dropCollection().observableValue
      collection.insertMany(createDocuments(100)).observableValue
      val documents = new ArrayBuffer[Document]()
      collection.find(new Document("_id", new Document("$gte", 50))).into(documents).observableValue
      documents.length should equal(50)
  }

  it should "be able to insert many items" in withCollection {
    collection =>
      val size = 50
      val observables: IndexedSeq[Observable[WriteConcernResult]] = for (i <- 0 until size) yield {
        val doc = new Document()
        doc.put("_id", s"rxscala-$i")
        doc.put("field", "Some value")
        collection.insertOne(doc)
      }
      observables.toObservable.flatten.toSeq.observableValue
      collection.count().observableValue should be(size)
  }

  it should "Should be able to call count" in withCollection {
    collection =>
      collection.insertMany(createDocuments(100)).observableValue
      val result = collection.count(new Document("_id", new Document("$gte", 50))).observableValue
      result should be(50)
  }

  it should "get indexes for a new collection" in withDatabase(collectionName) {
    database =>
      database.createCollection(collectionName).observableValue
      database(collectionName).indexes().observableList.length should equal(1)
  }

  it should "add index" in withCollection {
    collection =>
      collection.dropCollection().observableValue
      collection.createIndex(new Document("test", 1)).observableValue
      collection.indexes().observableList.length should equal(2)
  }

  it should "drop index for non-existent collection" in withCollection {
    collection =>
      collection.dropIndex("test").observableValue
      collection.dropIndex("test").observableValue
  }

  it should "drop index" in withCollection {
    collection =>
      collection.createIndex(new Document("test", 1)).observableValue
      collection.indexes().observableList.map { d => d.getString("name") } should contain("test_1")
      collection.dropIndex("test_1").observableValue

      collection.indexes().observableList.map {d => d.getString("name")} should not contain("test_1")
  }

  it should "drop indexes for non-existent collection" in withCollection {
    collection =>
      collection.dropCollection().observableValue
      collection.dropIndexes().observableValue
  }

  it should "drop indexes existing collection" in withCollection {
    collection =>
      collection.createIndex(new Document("test", 1)).observableValue
      collection.dropIndexes().observableValue
  }

  def createDocuments(amount: Int = 100): IndexedSeq[Document] = {
    for (i <- 0 until amount) yield new Document("_id", i)
  }
}

