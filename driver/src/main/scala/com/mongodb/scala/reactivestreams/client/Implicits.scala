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

package com.mongodb.scala.reactivestreams.client

import java.util.Date

import com.mongodb.scala.reactivestreams.client.Helpers.DefaultsTo
import com.mongodb.scala.reactivestreams.client.collection.immutable.{ Document => ImmutableDocument }
import com.mongodb.scala.reactivestreams.client.collection.mutable.{ Document => MutableDocument }
import com.mongodb.scala.reactivestreams.client.collection.immutable.Document
import org.bson._
import org.bson.types.ObjectId

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.{ Failure, Success, Try }

object Implicits extends FromAnyImplicits with IterableImplicits with DateTimeImplicits {

  implicit def immutableDocumentToBsonDocument(value: ImmutableDocument): BsonDocument = value.underlying

  implicit def bsonDocumentToImmutableDocument(value: BsonDocument): ImmutableDocument = ImmutableDocument(value)

  implicit def mutableDocumentToBsonDocument(value: MutableDocument): BsonDocument = value.underlying

  implicit def bsonDocumentToMutableDocument(value: BsonDocument): MutableDocument = MutableDocument(value)

  implicit def binaryArrayToBsonBinary(value: Array[Byte]): BsonBinary = new BsonBinary(value)

  implicit def bsonBinaryTobinaryArray(value: BsonBinary): Array[Byte] = value.getData

  implicit def booleanToBsonBoolean(value: Boolean): BsonBoolean = new BsonBoolean(value)

  implicit def bsonBooleanToBoolean(value: BsonBoolean): Boolean = value.getValue

  implicit def doubleToBsonDouble(value: Double): BsonDouble = new BsonDouble(value)

  implicit def bsonDoubleToDouble(value: BsonDouble): Double = value.doubleValue()

  implicit def intToBsonInt32(value: Int): BsonInt32 = new BsonInt32(value)

  implicit def bsonInt32ToInt(value: BsonInt32): Int = value.intValue()

  implicit def longToBsonInt64(value: Long): BsonInt64 = new BsonInt64(value)

  implicit def bsonInt64ToLong(value: BsonInt64): Long = value.longValue()

  implicit def noneToBsonNull(value: Option[Nothing]): BsonNull = new BsonNull()

  implicit def bsonNullToNone(value: BsonNull): Option[Nothing] = None

  implicit def objectIdToBsonObjectId(value: ObjectId): BsonObjectId = new BsonObjectId(value)

  implicit def bsonObjectIdToObjectId(value: BsonObjectId): ObjectId = value.getValue

  implicit def stringToBsonString(value: String): BsonString = new BsonString(value)

  implicit def bsonStringToString(value: BsonString): String = value.getValue

  implicit def symbolToBsonSymbol(value: Symbol): BsonSymbol = new BsonSymbol(value.name)

  implicit def bsonSymbolToSymbol(value: BsonSymbol): Symbol = Symbol(value.getSymbol)

  implicit def regexToBsonRegularExpression(value: Regex): BsonRegularExpression = new BsonRegularExpression(value.regex)

  implicit def bsonRegularExpression(value: BsonRegularExpression): Regex = new Regex(value.getPattern)

}

trait IterableImplicits {

  implicit def iterableToBsonArray[B](value: Iterable[B])(implicit e: B DefaultsTo BsonValue, ev: B => BsonValue): BsonArray =
    new BsonArray(value.map(ev).toList.asJava)

  implicit def bsonArrayToIterable(value: BsonArray): Iterable[BsonValue] = value.asScala

  implicit def bsonArrayToIterableBinary(value: BsonArray): Iterable[Array[Byte]] = value.asScala map (v => v.asBinary().getData)

  implicit def bsonArrayToIterableBoolean(value: BsonArray): Iterable[Boolean] = value.asScala map (v => v.asBoolean().getValue)

  implicit def bsonArrayToIterableDouble(value: BsonArray): Iterable[Double] = value.asScala map (v => v.asNumber().doubleValue())

  implicit def bsonArrayToIterableInt(value: BsonArray): Iterable[Int] = value.asScala map (v => v.asNumber().intValue())

  implicit def bsonArrayToIterableLong(value: BsonArray): Iterable[Long] = value.asScala map (v => v.asNumber().longValue())

  implicit def bsonArrayToIterableObjectId(value: BsonArray): Iterable[ObjectId] = value.asScala map (v => v.asObjectId().getValue)

  implicit def bsonArrayToIterableRegex(value: BsonArray): Iterable[Regex] = value.asScala map (v => new Regex(v.asRegularExpression().getPattern))

  implicit def bsonArrayToIterableString(value: BsonArray): Iterable[String] = value.asScala map (v => v.asString().getValue)

  implicit def bsonArrayToIterableSymbol(value: BsonArray): Iterable[Symbol] = value.asScala map (v => Symbol(v.asSymbol().getSymbol))

  implicit def bsonArrayToIterableDocument(value: BsonArray): Iterable[Document] = value.asScala map (v => Document(v.asDocument()))

  implicit def bsonArrayToIterableIterable(value: BsonArray): Iterable[Iterable[BsonValue]] = value.asScala map (v => v.asArray().getValues.asScala)

  implicit def bsonArrayToIterableNone(value: BsonArray): Iterable[Option[Nothing]] = {
    value.asScala.foreach(v => if (!v.isNull()) {
      throw new BsonInvalidOperationException(s"Invalid type cannot be converted to a BsonValue: $v")
    })
    value.asScala.map(v => None)
  }
}

trait DateTimeImplicits {
  implicit def dateTimeToBsonDateTime(value: Date): BsonDateTime = new BsonDateTime(value.getTime)

  implicit def bsonDateTimeToDate(value: BsonDateTime): Date = new Date(value.getValue)

  implicit def bsonArrayToIterableDateTime(value: BsonArray): Iterable[Date] = value.asScala map (v => new Date(v.asDateTime().getValue))
}

trait FromAnyImplicits {

  // scalastyle:off cyclomatic.complexity
  implicit def anyToBsonValue(v: Any): BsonValue = {
    v match {
      case x @ (v: BsonValue)         => v
      case x @ (v: Array[Byte])       => new BsonBinary(v)
      case x @ (v: Boolean)           => new BsonBoolean(v)
      case x @ (v: Date)              => new BsonDateTime(v.getTime)
      case x @ (v: Double)            => new BsonDouble(v)
      case x @ (v: Int)               => new BsonInt32(v)
      case x @ (v: Long)              => new BsonInt64(v)
      case x @ (v: ObjectId)          => new BsonObjectId(v)
      case x @ (v: String)            => new BsonString(v)
      case x @ (v: Symbol)            => new BsonSymbol(v.name)
      case x @ (v: Regex)             => new BsonRegularExpression(v.regex)
      case None                       => new BsonNull
      case x @ (v: ImmutableDocument) => v.underlying
      case x @ (v: MutableDocument)   => v.underlying
      case x @ (v: Iterable[_]) => {
        Try(iterableAnyToBsonArray(v)) match {
          case Success(bsonValue) => bsonValue
          case Failure(ex)        => throw new BsonInvalidOperationException(s"Invalid type cannot be converted to a BsonValue: $v")
        }
      }
      case _ =>
        throw new BsonInvalidOperationException(s"Invalid type cannot be converted to a BsonValue: $v")
    }
  }

  // scalastyle:on cyclomatic.complexity

  private def iterableAnyToBsonArray[B](value: Iterable[B])(implicit e: B DefaultsTo BsonValue, ev: B => BsonValue): BsonArray =
    new BsonArray(value.map(ev).toList.asJava)
}
