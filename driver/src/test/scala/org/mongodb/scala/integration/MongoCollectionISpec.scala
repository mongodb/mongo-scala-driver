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
 * https://github.com/mongodb/mongo-scala-driver
 *
 */
package org.mongodb.scala.integration


import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import rx.lang.scala.{Observable, Subject}

import org.mongodb.Document

import org.mongodb.scala._
import org.mongodb.scala.Implicits._
import org.mongodb.scala.helpers.RequiresMongoDBSpec

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

  it should "provide an Subject cursor" in withCollection {
    collection =>
      collection.cursor shouldBe a[Subject[Document]]
  }

  it should "aggregated observables should be convertible to a Future" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      val result = collection.cursor.foldLeft(0)({
        (total, doc) => total + 1
      }).toFuture
      result.futureValue should equal(100)
  }

  it should "cursor.toList should return Future[List[Document]]" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      val documents = collection.toList
      documents shouldBe a[Future[List[Document]]]
      documents.futureValue.length should equal(100)
  }

  it should "be a composable observer and provide the expected results" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      collection.count().futureValue should equal(100)

      var total = 0
      val counterObservable: Observable[Document] = collection.cursor.doOnEach(d => {
        total += 1
      })
      total should not equal 100 // Ensures foreach is non blocking
      counterObservable.toBlockingObservable.toList
      total should equal(100)
  }

  it should "allow scala like handling for filtered collections" in withCollection {
    collection =>
      collection.insert(createDocuments(100)).futureValue
      val filtered = collection.find(new Document("_id", new Document("$gte", 50))).cursor
      val result: Observable[Int] = filtered.foldLeft(0)({
        (total, doc) => total + 1
      })
      result.toFuture.futureValue should equal(50)
  }

  def createDocuments(amount: Int = 100): IndexedSeq[Document] = {
    for (i <- 0 until amount) yield new Document("_id", i)
  }
}

