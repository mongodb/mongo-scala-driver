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


import scala.concurrent.duration._
import scala.language.postfixOps

import org.mongodb.Document

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.helpers.RequiresMongoDBSpec

class MongoCollectionISpec extends RequiresMongoDBSpec {

  lazy val duration = Duration("5 sec")

  "MongoCollection" should "be callable via apply" in withCollection {
    collection =>
      collection shouldBe a[MongoCollection[_]]
  }

  it should "be able to get a count" in withCollection {
    collection =>
      collection.count().futureValue === 0
  }

  it should "be able to insert a document" in withCollection {
    collection =>
      collection.admin.drop().futureValue
      collection.insert(new Document("Hello", "World")).isReadyWithin(100 millis)
      collection.count().futureValue === 1
  }
}

