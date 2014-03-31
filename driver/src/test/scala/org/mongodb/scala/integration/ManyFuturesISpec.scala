package org.mongodb.scala.integration


import scala.collection.immutable.IndexedSeq
import scala.concurrent._

import org.mongodb.{Document, WriteResult}

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.helpers.RequiresMongoDBSpec

class ManyFuturesISpec extends RequiresMongoDBSpec {

  "MongoCollection" should "be callable via apply" in withCollection {
    collection =>
      collection shouldBe a[MongoCollection[_]]
  }

  it should "be able to get a count" in withCollection {
    collection =>
      collection.count().futureValue should equal(0)
  }

  it should "be able to insert many items" in withCollection {
    collection =>

      val size = 50
      val futures: IndexedSeq[Future[WriteResult]] = for (i <- 0 until size) yield {
        val doc = new Document()
        doc.put("_id", i)
        doc.put("field", "Some value")
        collection.insert(doc)
      }
      Future.sequence(futures).futureValue
      collection.count().futureValue should be(size)
  }

}
