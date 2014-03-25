package org.mongodb.scala.integration


import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration.Duration

import org.mongodb.{Document, WriteResult}

import org.mongodb.scala.helpers.RequiresMongoDBSpec


class ManyFuturesISpec extends RequiresMongoDBSpec {

  "Collections" should "be able to insert many items" in {
    checkMongoDB()
    val collection = mongoClient("test")("ManyFutures")
    Await.result(collection.admin.drop(), Duration.Inf)

    val futures = ListBuffer[Future[WriteResult]]()

    val size = 500

    for(i <- 0 until size) {
      val doc = new Document()
      doc.put("_id", i)
      doc.put("field", "Some value")
      futures += collection.insert(doc)
    }
    val allFutures = Future.sequence(futures)
    Await.ready(allFutures, Duration.Inf)
    Await.result(collection.count(), Duration.Inf) should be(size)
  }

}
