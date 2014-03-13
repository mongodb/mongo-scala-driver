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
/*

package org.mongodb.scala.test.api

import org.specs2.mutable._
import org.specs2.specification.{Step, Fragments}

import scala.concurrent._
import scala.concurrent.duration.Duration

import org.mongodb.connection.ServerAddress
import org.mongodb.Document

import play.api.libs.iteratee.{Enumerator, Iteratee}
import org.mongodb.scala.MongoClient


class IterateesSpec extends Specification {
  lazy val client = MongoClient(new ServerAddress("localhost:27017"))
  lazy val db = client("MongoScala")
  lazy val collection = db("test")
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def setup() {
    val setup = Future {
      var documents: List[Document] = List()
      for (i <- 1 to 10) documents = documents :+ new Document("_id", i)
      db.admin.drop() ->
      collection.insert(documents)
    }
    Await.ready(setup, Duration.Inf)
  }

  def teardown() {
    val teardown = Future {
      db.admin.drop() ->
      client.close()
    }
    Await.ready(teardown, Duration.Inf)
  }

  override def map(fs: => Fragments) = Step(setup()) ^ fs ^ Step(teardown())

  sequential

  "MongoDB Collection" should {
    "provide an Enumerator cursor" in {
      collection.cursor must beAnInstanceOf[Enumerator[Document]]
    }

    "cursor should be non-blocking" in {
      val result: Future[Int] = collection.cursor |>>> Iteratee.fold[Document, Int](0) {
        (total, doc) => total + 1
      }
      Await.result(result, Duration.Inf) must equalTo(10)
    }

    "cursor.toList should return Future[List[Document]]" in {
      collection.toList must beAnInstanceOf[Future[List[Document]]]
      Await.result(collection.toList, Duration.Inf).length must beEqualTo(10)
    }

    "allow scala like handling of a futuristic foreach" in {
      collection.count must beEqualTo(10).await
      var count = 0
      val counter = collection.foreach[Int](d => {
        count += 1
        count
      })
      count must not be_!== 10  // Ensures foreach is non blocking
      counter.unsubscribe()
      count must beEqualTo(10)
    }

    "allow scala like handling of an inline iteratee foreach" in {
      collection.count must beEqualTo(10).await
      var count = 0
      val counter = Iteratee.foreach[Document](d => count += 1)
      val futureCount = collection.cursor |>>> counter
      Await.result(futureCount, Duration.Inf)
      assert(count==10)
      count must beEqualTo(10)
    }


    "allow scala like handling for filtered collections" in {
      val filtered = collection.find(new Document("_id", new Document("$gt", 5))).cursor
      val result: Future[Int] = filtered |>>> Iteratee.fold[Document, Int](0) {
        (total, doc) => total + 1
      }
      result must beAnInstanceOf[Future[Int]]
      Await.result(result, Duration.Inf) must equalTo(5)
    }

    "allow iteratee folding of collections" in {
      val result = collection.fold[Int](0) {
        (total, doc) => total + 1
      }
      result must beAnInstanceOf[Future[Int]]
      //Await.result(result, Duration.Inf) must equalTo(10)
    }

    "allow custom iteratee folding of collections" in {
      val cursor = collection.cursor
      val fold = Iteratee.fold[Document, Int](0) {
        (total, doc) => total + 1
      }
      val result = cursor |>>> fold
      result must beAnInstanceOf[Future[Int]]
      Await.result(result, Duration.Inf) must equalTo(10)
    }

  }
}
*/
