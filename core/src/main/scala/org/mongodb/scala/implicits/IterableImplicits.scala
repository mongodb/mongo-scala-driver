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

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import org.bson.types.ObjectId
import org.bson.{Document => JDocument, _}
import org.mongodb.scala.Helpers.DefaultsTo
import org.mongodb.scala.collection.{immutable, mutable}

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

  implicit def bsonArrayToIterableImmutableDocument(value: BsonArray): Iterable[immutable.Document] = value.asScala map (v =>
    immutable.Document(v.asDocument()))

  implicit def bsonArrayToIterableMutableDocument(value: BsonArray): Iterable[mutable.Document] = value.asScala map (v =>
    mutable.Document(v.asDocument()))

  implicit def bsonArrayToIterableIterable(value: BsonArray): Iterable[Iterable[BsonValue]] = value.asScala map (v => v.asArray().getValues.asScala)

  implicit def bsonArrayToIterableNone(value: BsonArray): Iterable[Option[Nothing]] = {
    value.asScala.foreach(v => if (!v.isNull()) {
      throw new BsonInvalidOperationException(s"Invalid type cannot be converted to a BsonValue: $v")
    })
    value.asScala.map(v => None)
  }
}
