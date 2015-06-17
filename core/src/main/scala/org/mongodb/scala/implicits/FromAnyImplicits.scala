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

import java.util.Date

import scala.language.implicitConversions

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

import org.bson.types.ObjectId
import org.bson.{Document => JDocument, _}
import org.mongodb.scala.Helpers.DefaultsTo
import org.mongodb.scala.collection.{immutable, mutable}

trait FromAnyImplicits {

  // scalastyle:off cyclomatic.complexity
  implicit def anyToBsonValue(v: Any): BsonValue = {
    v match {
      case x@(v: BsonValue) => v
      case x@(v: Array[Byte]) => new BsonBinary(v)
      case x@(v: Boolean) => new BsonBoolean(v)
      case x@(v: Date) => new BsonDateTime(v.getTime)
      case x@(v: Double) => new BsonDouble(v)
      case x@(v: Int) => new BsonInt32(v)
      case x@(v: Long) => new BsonInt64(v)
      case x@(v: ObjectId) => new BsonObjectId(v)
      case x@(v: String) => new BsonString(v)
      case x@(v: Symbol) => new BsonSymbol(v.name)
      case x@(v: Regex) => new BsonRegularExpression(v.regex)
      case None => new BsonNull
      case x@(v: immutable.Document) => v.underlying
      case x@(v: mutable.Document) => v.underlying
      case x@(v: Iterable[_]) => {
        Try(iterableAnyToBsonArray(v)) match {
          case Success(bsonValue) => bsonValue
          case Failure(ex) => throw new BsonInvalidOperationException(s"Invalid type cannot be converted to a BsonValue: $v")
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
