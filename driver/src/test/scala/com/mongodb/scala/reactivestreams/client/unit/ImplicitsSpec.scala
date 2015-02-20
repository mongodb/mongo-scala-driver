/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.scala.reactivestreams.client.unit

import java.util.Date

import com.mongodb.scala.reactivestreams.client.Implicits._
import com.mongodb.scala.reactivestreams.client.collection.Document
import com.mongodb.scala.reactivestreams.client.collection.immutable.{ Document => ImmutableDocument }
import com.mongodb.scala.reactivestreams.client.collection.mutable.{ Document => MutableDocument }
import org.bson._
import org.bson.types.ObjectId
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._
import scala.util.matching.Regex

class ImplicitsSpec extends FlatSpec with Matchers {

  "Implicits" should "convert to and from BsonDocument to immutable documents" in {
    val doc = ImmutableDocument("a" -> "a", "b" -> ImmutableDocument("c" -> 1))
    val bsonDoc = new BsonDocument("a", new BsonString("a"))
    bsonDoc.put("b", new BsonDocument("c", new BsonInt32(1)))

    val bsonDocFromDoc: BsonDocument = doc
    bsonDocFromDoc should equal(bsonDoc)

    val docFromBsonDocument: ImmutableDocument = bsonDoc
    docFromBsonDocument should equal(doc)
  }

  it should "convert to and from BsonDocument to mutable documents" in {
    val doc = MutableDocument("a" -> "a", "b" -> MutableDocument("c" -> 1))
    val bsonDoc = new BsonDocument("a", new BsonString("a"))
    bsonDoc.put("b", new BsonDocument("c", new BsonInt32(1)))

    val bsonDocFromDoc: BsonDocument = doc
    bsonDocFromDoc should equal(bsonDoc)

    val docFromBsonDocument: MutableDocument = bsonDoc
    docFromBsonDocument should equal(doc)
  }

  it should "convert to and from BsonBinary" in {
    val expected: Array[Byte] = "bson".toCharArray map (_.toByte)

    val bsonToNative: Array[Byte] = new BsonBinary(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonBinary = bsonToNative
    nativeToBson.getData should equal(expected)
  }

  it should "convert to and from BsonBoolean" in {
    val expected: Boolean = true

    val bsonToNative: Boolean = new BsonBoolean(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonBoolean = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonDateTime" in {
    val expected: Date = new Date()

    val bsonToNative: Date = new BsonDateTime(expected.getTime)
    bsonToNative should equal(expected)

    val nativeToBson: BsonDateTime = bsonToNative
    nativeToBson.getValue should equal(expected.getTime)
  }

  it should "convert to and from BsonDouble" in {
    val expected: Double = 2.0

    val bsonToNative: Double = new BsonDouble(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonDouble = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonInt32" in {
    val expected: Int = 2

    val bsonToNative: Int = new BsonInt32(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonInt32 = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonInt64" in {
    val expected: Long = 2L

    val bsonToNative: Long = new BsonInt64(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonInt64 = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonNull" in {
    val expected = None

    val bsonToNative: Option[Nothing] = new BsonNull()
    bsonToNative should equal(expected)

    val nativeToBson: BsonNull = bsonToNative
    nativeToBson should equal(new BsonNull())
  }

  it should "convert to and from BsonObjectId" in {
    val expected: ObjectId = new ObjectId()

    val bsonToNative: ObjectId = new BsonObjectId(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonObjectId = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonRegularExpression" in {
    val expected: Regex = new Regex("^bson")

    val bsonToNative: Regex = new BsonRegularExpression(expected.regex)
    bsonToNative.regex should equal(expected.regex)

    val nativeToBson: BsonRegularExpression = bsonToNative
    nativeToBson.regex should equal(expected.regex)
  }

  it should "convert to and from BsonString" in {
    val expected: String = "bson"

    val bsonToNative: String = new BsonString(expected)
    bsonToNative should equal(expected)

    val nativeToBson: BsonString = bsonToNative
    nativeToBson.getValue should equal(expected)
  }

  it should "convert to and from BsonSymbol" in {
    val expected: Symbol = 'bson

    val bsonToNative: Symbol = new BsonSymbol(expected.name)
    bsonToNative should equal(expected)

    val nativeToBson: BsonSymbol = bsonToNative
    nativeToBson.getSymbol should equal(expected.name)
  }

  "BsonArrays" should "convert to and from BsonValues" in {
    val expected: Iterable[BsonValue] = List(
      new BsonBinary("bson".toCharArray map (_.toByte)),
      new BsonBoolean(true), new BsonDateTime(new Date().getTime), new BsonDouble(1.0), new BsonInt32(1), new BsonInt64(1L),
      new BsonNull(), new BsonObjectId(new ObjectId()), new BsonRegularExpression("^bson"), new BsonString("bson"),
      new BsonSymbol("bson"), new BsonDocument("a", new BsonString("b")), new BsonArray(List(new BsonString("a")).asJava)
    )

    val bsonToNative: Iterable[BsonValue] = new BsonArray(expected.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expected)
  }

  it should "convert to and from BsonArray containing BsonBinary" in {
    val expected: Iterable[Array[Byte]] = List("bson".toCharArray map (_.toByte), "array".toCharArray map (_.toByte))
    val expectedBson: Iterable[BsonValue] = expected map (new BsonBinary(_))

    val bsonToNative: Iterable[Array[Byte]] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Array[Byte]] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonBoolean" in {
    val expected: Iterable[Boolean] = List(true, false, true)
    val expectedBson: Iterable[BsonValue] = expected map (new BsonBoolean(_))

    val bsonToNative: Iterable[Boolean] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Boolean] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonDateTime" in {
    val expected: Iterable[Date] = List(new Date())
    val expectedBson: Iterable[BsonValue] = expected map (v => new BsonDateTime(v.getTime))

    val bsonToNative: Iterable[Date] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Date] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonDouble" in {
    val expected: Iterable[Double] = List(1.0, 2.0)
    val expectedBson: Iterable[BsonValue] = expected map (new BsonDouble(_))

    val bsonToNative: Iterable[Double] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Double] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonInt32" in {
    val expected: Iterable[Int] = List(1, 2, 3)
    val expectedBson: Iterable[BsonValue] = expected map (new BsonInt32(_))

    val bsonToNative: Iterable[Int] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Int] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonInt64" in {
    val expected: Iterable[Long] = List(1L, 2L, 3L)
    val expectedBson: Iterable[BsonValue] = expected map (new BsonInt64(_))

    val bsonToNative: Iterable[Long] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Long] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonNull" in {
    val expected: Iterable[Option[Nothing]] = List(None, None, None)
    val expectedBson: Iterable[BsonValue] = expected map (v => new BsonNull())

    val bsonToNative: Iterable[Option[Nothing]] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Option[Nothing]] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonObjectId" in {
    val expected: Iterable[ObjectId] = List(new ObjectId(), new ObjectId())
    val expectedBson: Iterable[BsonValue] = expected map (new BsonObjectId(_))

    val bsonToNative: Iterable[ObjectId] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[ObjectId] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonRegularExpression" in {
    val expected: Iterable[Regex] = List(new Regex("^bson"), "^array".r)
    val expectedBson: Iterable[BsonValue] = expected map (v => new BsonRegularExpression(v.getPattern))

    val bsonToNative: Iterable[Regex] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative.map(v => v.getPattern()) should equal(expected.map(v => v.getPattern()))

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Regex] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonString" in {
    val expected: Iterable[String] = List("bson", "array")
    val expectedBson: Iterable[BsonValue] = expected map (new BsonString(_))

    val bsonToNative: Iterable[String] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[String] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonSymbol" in {
    val expected: Iterable[Symbol] = List('bson, Symbol("array"))
    val expectedBson: Iterable[BsonValue] = expected map (v => new BsonSymbol(v.name))

    val bsonToNative: Iterable[Symbol] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Symbol] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert to and from BsonArray containing BsonArray" in {
    val expected: Iterable[Iterable[BsonValue]] = List(
      List(new BsonBinary("bson".toCharArray map (_.toByte)), new BsonBoolean(true), new BsonDateTime(new Date().getTime)),
      List(new BsonDouble(1.0), new BsonInt32(1), new BsonInt64(1L), new BsonNull(), new BsonObjectId(new ObjectId())),
      List(new BsonRegularExpression("^bson"), new BsonString("bson"), new BsonSymbol("bson")),
      List(new BsonDocument("a", new BsonString("b")), new BsonArray(List(new BsonString("a")).asJava))
    )
    val expectedBson: Iterable[BsonArray] = expected.map(v => new BsonArray(v.toList.asJava))

    val bsonToNative: Iterable[Iterable[BsonValue]] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Iterable[BsonValue]] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }

  }

  it should "convert to and from BsonArray containing BsonDocuments" in {
    val expected: Iterable[Document] = List(Document("a" -> 1), Document("b" -> 2), Document("c" -> 3))
    val expectedBson: Iterable[BsonDocument] = expected.map(v => v.toBsonDocument)

    val bsonToNative: Iterable[Document] = new BsonArray(expectedBson.toList.asJava)
    bsonToNative should equal(expected)

    val nativeToBson: BsonArray = bsonToNative
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

    intercept[BsonInvalidOperationException] {
      val shouldFail: Iterable[Document] = new BsonArray(List(new BsonString("a"), new BsonBoolean(false)).asJava)
    }
  }

  it should "convert an Iterable[_] to BsonArray" in {
    val bytes = "bson".toCharArray map (_.toByte)
    val bool = true
    val date = new Date()
    val double = 1.0
    val int = 1
    val long = 1L
    val objectId = new ObjectId()
    val regex = "^bson".r
    val string = "bson"
    val symbol = 'bson
    val document = Document("a" -> new BsonString("b"))
    val array = List(new BsonString(string), new BsonBoolean(bool))

    val native = List(bytes, bool, date, double, int, long, None, objectId, regex, string, symbol, document, array)
    val expectedBson: Iterable[BsonValue] = List(
      new BsonBinary(bytes),
      new BsonBoolean(bool), new BsonDateTime(date.getTime), new BsonDouble(double), new BsonInt32(int), new BsonInt64(long),
      new BsonNull(), new BsonObjectId(objectId), new BsonRegularExpression(regex.regex), new BsonString(string),
      new BsonSymbol(symbol.name), document.toBsonDocument, new BsonArray(array.asJava)
    )

    val nativeToBson: BsonArray = native
    nativeToBson.getValues should contain theSameElementsInOrderAs (expectedBson)

  }

  "anyToBsonValue" should "throw exceptions when passed invalid types" in {
    intercept[BsonInvalidOperationException] {
      val shouldFail: BsonValue = Some(true)
    }

    intercept[BsonInvalidOperationException] {
      val shouldFail: BsonValue = List(Some(true), None)
    }
  }
}
