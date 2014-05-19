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
package org.mongodb.scala.async.integration


import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps

import org.mongodb.{Document, WriteResult}

import org.mongodb.scala.async.MongoCollection
import org.mongodb.scala.async.helpers.RequiresMongoDBSpec

class MongoCollectionISpec extends RequiresMongoDBSpec {

  lazy val duration = Duration("5 sec")

  "MongoCollection" should "be callable via apply" in withCollection {
    collection =>
      collection shouldBe a[MongoCollection[_]]
  }

  it should "be able to get a count" in withCollection {
    collection =>
      collection.count().futureValue should equal(0)
  }

  it should "be able to insert a document" in withCollection {
    collection =>
      collection.admin.drop().futureValue
      collection.insert(new Document("Hello", "World")).futureValue
      collection.count().futureValue should equal(1)
  }

  it should "cursor.toList() should return Future[List[Document]]" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      val documents = collection.toList()
      documents shouldBe a[Future[List[Document]]]
      documents.futureValue.length should equal(100)
  }

  it should "cursor should be non blocking and provide the expected results" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      var total = 0
      val result = collection.toList()
      total should not equal 100 // Ensures foreach is non blocking
      total = result.futureValue.size // Complete the future
      total should equal(100)
  }

  it should "allow scala like handling for filtered collections" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      val filtered = collection.find(new Document("_id", new Document("$gte", 50)))
      filtered.toList().asInstanceOf[Future[List[Document]]].futureValue.size should equal(50)
  }

  it should "be able to insert many items" in withCollection {
    collection =>
      val size = 500
      val futures: IndexedSeq[Future[WriteResult]] = for (i <- 0 until size) yield {
        val doc = new Document()
        doc.put("_id", i)
        doc.put("field", "Some value")
        collection.insert(doc)
      }
      Future.sequence(futures).futureValue
      collection.count().futureValue should be(size)
  }

  def createDocuments(amount: Int = 100): IndexedSeq[Document] = {
    for (i <- 0 until amount) yield new Document("_id", i)
  }
}

