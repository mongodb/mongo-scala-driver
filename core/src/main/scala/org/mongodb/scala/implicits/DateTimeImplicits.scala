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

import org.bson.{BsonArray, BsonDateTime}

trait DateTimeImplicits {
  implicit def dateTimeToBsonDateTime(value: Date): BsonDateTime = new BsonDateTime(value.getTime)

  implicit def bsonDateTimeToDate(value: BsonDateTime): Date = new Date(value.getValue)

  implicit def bsonArrayToIterableDateTime(value: BsonArray): Iterable[Date] = value.asScala map (v => new Date(v.asDateTime().getValue))
}
