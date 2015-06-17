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

package org.mongodb.scala.implicits

import scala.language.implicitConversions

import scala.util.matching.Regex

import org.bson.types.ObjectId
import org.bson.{Document => JDocument, _}
import org.mongodb.scala.collection.{immutable, mutable}

trait BsonImplicits {

  implicit def immutableDocumentToBsonDocument(value: immutable.Document): BsonDocument = value.underlying

  implicit def bsonDocumentToImmutableDocument(value: BsonDocument): immutable.Document = immutable.Document(value)

  implicit def mutableDocumentToBsonDocument(value: mutable.Document): BsonDocument = value.underlying

  implicit def bsonDocumentToMutableDocument(value: BsonDocument): mutable.Document = mutable.Document(value)

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


