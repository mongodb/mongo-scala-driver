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
package org.mongodb.scala.async.perf

import java.util.logging.{Level, Logger}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import org.scalameter.api._

import org.mongodb.{Document, WriteResult}

import org.mongodb.scala.async._

class InsertBatchBenchmark extends PerformanceTest.Quickbenchmark {

  val sizes = Gen.enumeration("BatchSize")(1, 10, 100, 1000)
  val count = 2000
  val fillerString = "*" * 400
  lazy val collection = MongoClient("mongodb://rozza/?waitQueueMultiple=100")("mongo-scala-perf")(this.getClass.getSimpleName)

  performance of "Insert" in {
    measure method "With a Simple Document" in {
      using(sizes) config(
        exec.benchRuns -> 10,
        exec.independentSamples -> 3
        ) beforeTests {
        // Turn off org.mongodb's noisy connection INFO logging - only works with the JULLogger
        Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.WARNING)
        Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.WARNING)
        Await.result(collection.admin.drop(), Duration.Inf)
      } afterTests {
        Await.result(collection.admin.drop(), Duration.Inf)
      } in {
        size =>
          val loops = count / size
          val futures: IndexedSeq[Future[WriteResult]] = (0 until loops).map(_ => {
            val documents: IndexedSeq[Document] = (0 until size).map(_ => new Document("filler", fillerString))
            collection.insert(documents)
          })
          Await.result(Future.sequence(futures), Duration.Inf)
      }
    }
  }

}


