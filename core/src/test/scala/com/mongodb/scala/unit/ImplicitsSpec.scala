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

package com.mongodb.scala.unit

import java.util.Date

import scala.language.implicitConversions

import scala.util.matching.Regex

import org.bson._
import org.bson.types.ObjectId
import com.mongodb.scala.Implicits._
import com.mongodb.scala.collection.{immutable, mutable}

import org.scalatest.{FlatSpec, Matchers}

class ImplicitsSpec extends FlatSpec with Matchers {

  "Implicits" should "convert boolean to BsonBoolean and back" in {
    val nativeValue = true
    val bsonValue = new BsonBoolean(true)

    val bsonImplicit: BsonBoolean = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Boolean = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert a binary Array to a BsonBinary and back" in {
    val nativeValue = Array[Byte](1, 2, 3)
    val bsonValue = new BsonBinary(nativeValue)

    val bsonImplicit: BsonBinary = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Array[Byte] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert dateTime to BsonDateTime and back" in {
    val nativeValue = new Date(1)
    val bsonValue = new BsonDateTime(1)

    val bsonImplicit: BsonDateTime = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Date = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert double to BsonDouble and back" in {
    val nativeValue = 2.0
    val bsonValue = new BsonDouble(2.0)

    val bsonImplicit: BsonDouble = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Double = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert int to BsonInt32 and back" in {
    val nativeValue = 2
    val bsonValue = new BsonInt32(2)

    val bsonImplicit: BsonInt32 = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Int = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert long to BsonInt64 and back" in {
    val nativeValue = 2L
    val bsonValue = new BsonInt64(2)

    val bsonImplicit: BsonInt64 = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Long = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert None to BsonNull and back" in {
    val nativeValue = None
    val bsonValue = new BsonNull()

    val bsonImplicit: BsonNull = nativeValue
    bsonImplicit should equal(bsonValue)

    // None is a case object of Option[Nothing]
    val nativeImplicit: Option[Nothing] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert objectId to BsonObjectId and back" in {
    val nativeValue = new ObjectId()
    val bsonValue = new BsonObjectId(nativeValue)

    val bsonImplicit: BsonObjectId = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: ObjectId = bsonValue
    nativeImplicit should equal(nativeValue)
  }
  it should "convert regex to BsonRegularExpression and back" in {
    val nativeValue = new Regex("(.*)")
    val bsonValue = new BsonRegularExpression("(.*)")

    val bsonImplicit: BsonRegularExpression = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Regex = bsonValue
    nativeImplicit.regex should equal(nativeValue.regex)
  }

  it should "convert string to BsonString and back" in {
    val nativeValue = "String"
    val bsonValue = new BsonString(nativeValue)

    val bsonImplicit: BsonString = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: String = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert symbol to BsonSymbol and back" in {
    val nativeValue = 'symbol
    val bsonValue = new BsonSymbol(nativeValue.name)

    val bsonImplicit: BsonSymbol = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Symbol = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert immutable Document to BsonDocument and back" in {
    val nativeValue = immutable.Document("a" -> 1, "b" -> "two", "c" -> false)
    val bsonValue = nativeValue.toBsonDocument

    val bsonImplicit: BsonDocument = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: immutable.Document = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert mutable Document to BsonDocument and back" in {
    val nativeValue = mutable.Document("a" -> 1, "b" -> "two", "c" -> false)
    val bsonValue = nativeValue.toBsonDocument

    val bsonImplicit: BsonDocument = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: mutable.Document = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Boolean] to BsonArray and back" in {
    val nativeValue = List(true, false, true)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonBoolean))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Boolean] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Array[Byte]] to BsonArray and back" in {
    val nativeValue = List(Array[Byte](1,2,3), Array[Byte](1,2,3))
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonBinary))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Array[Byte]] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Int] to BsonArray and back" in {
    val nativeValue = List(1, 2, 3)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonInt32))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Int] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Long] to BsonArray and back" in {
    val nativeValue = List(1L, 2L, 3L)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonInt64))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Long] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Double] to BsonArray and back" in {
    val nativeValue = List(1.0, 2.0, 3.0)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonDouble))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Double] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Date] to BsonArray and back" in {
    val nativeValue = List(new Date(1), new Date(2))
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonDateTime))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Date] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[String] to BsonArray and back" in {
    val nativeValue = List("a", "b", "c")
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonString))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[String] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[ObjectId] to BsonArray and back" in {
    val nativeValue = List(new ObjectId(), new ObjectId())
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonObjectId))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[ObjectId] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Null] to BsonArray and back" in {
    val nativeValue = List(None, None, None)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonNull))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Option[Nothing]] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Regex] to BsonArray and back" in {
    val nativeValue = List(new Regex(".*"), new Regex(".*"))
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonRegularExpression))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Regex] = bsonValue
    nativeImplicit.map(_.regex) should equal(nativeValue.map(_.regex))
  }

  it should "convert Iterable[Symbol] to BsonArray and back" in {
    val nativeValue = List('a, 'b)
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonSymbol))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Symbol] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[immutableDocument] to BsonArray and back" in {
    val nativeValue = List(immutable.Document("a" -> 1, "b" -> "two", "c" -> false))
    val bsonValue = new BsonArray(nativeValue.map(_.toBsonDocument))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[immutable.Document] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[mutableDocument] to BsonArray and back" in {
    val nativeValue = List(mutable.Document("a" -> 1, "b" -> "two", "c" -> false))
    val bsonValue = new BsonArray(nativeValue.map(_.toBsonDocument))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[mutable.Document] = bsonValue
    nativeImplicit should equal(nativeValue)
  }

  it should "convert Iterable[Iterable[Any]] to BsonArray and back" in {
    val nativeValue = List(List(1, 2, 3), List(1, 2, 3))
    val bsonValue = new BsonArray(nativeValue.map(v => v: BsonValue))

    val bsonImplicit: BsonArray = nativeValue
    bsonImplicit should equal(bsonValue)

    val nativeImplicit: Iterable[Iterable[BsonValue]] = bsonValue
    nativeImplicit should equal(nativeValue.map(v => v.map(x => x: BsonValue)))
  }

 it should "convert bsonArray of mixed types to Iterable and back" in {
   val nativeValue = List(1, 2L, 3.0, "four", false, Array[Byte](1, 2, 3), new Date(1), new ObjectId(), ".*".r, 'symbol, None,
     immutable.Document("a" -> 1), List(1, 2, 3))
   val bsonValue = new BsonArray(nativeValue.map(v => v: BsonValue))

   val bsonImplicit: BsonArray = nativeValue
   bsonImplicit should equal(bsonValue)

   // No way back to Iterable[Any]
   val nativeImplicit: Iterable[BsonValue] = bsonValue
   nativeImplicit should equal(nativeValue.map(v => v: BsonValue))
 }

  it should "thrown an error when trying to convert invalid types" in {
    val nativeValue = List(None, Some(1))
    an[BsonInvalidOperationException] should be thrownBy({val bsonImplicit: BsonArray = nativeValue})

    val bsonValue = new BsonArray(List(new BsonInt32(1)))
    an[BsonInvalidOperationException] should be thrownBy({val nativeImplicit: Iterable[Option[Nothing]] = bsonValue})
  }

}
