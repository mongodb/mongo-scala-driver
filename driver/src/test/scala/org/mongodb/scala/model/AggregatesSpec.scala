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

package org.mongodb.scala.model

import java.lang.reflect.Modifier._

import org.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson

import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._
import org.scalatest.{ FlatSpec, Matchers }

class AggregatesSpec extends FlatSpec with Matchers {
  val registry = MongoClient.DEFAULT_CODEC_REGISTRY

  def toBson(bson: Bson): Document = Document(bson.toBsonDocument(classOf[BsonDocument], MongoClient.DEFAULT_CODEC_REGISTRY))

  "Aggregates" should "have the same methods as the wrapped Aggregates" in {
    val wrapped = classOf[com.mongodb.client.model.Aggregates].getDeclaredMethods
      .filter(f => isStatic(f.getModifiers) && isPublic(f.getModifiers)).map(_.getName).toSet
    val aliases = Set("filter")
    val local = Aggregates.getClass.getDeclaredMethods.filter(f => isPublic(f.getModifiers)).map(_.getName).toSet -- aliases
    local should equal(wrapped)
  }

  it should "have the same methods as the wrapped Accumulators" in {
    val wrapped = classOf[com.mongodb.client.model.Accumulators].getDeclaredMethods
      .filter(f => isStatic(f.getModifiers) && isPublic(f.getModifiers)).map(_.getName).toSet
    val local = Accumulators.getClass.getDeclaredMethods.filter(f => isPublic(f.getModifiers)).map(_.getName).toSet
    local should equal(wrapped)
  }

  it should "should render $match" in {
    toBson(`match`(Filters.eq("author", "dave"))) should equal(Document("""{ $match : { author : "dave" } }"""))
    toBson(filter(Filters.eq("author", "dave"))) should equal(Document("""{ $match : { author : "dave" } }"""))
  }

  it should "should render $project" in {
    toBson(project(fields(Projections.include("title", "author"), computed("lastName", "$author.last")))) should equal(
      Document("""{ $project : { title : 1 , author : 1, lastName : "$author.last" } }""")
    )
  }

  it should "should render $sort" in {
    toBson(sort(ascending("title", "author"))) should equal(Document("""{ $sort : { title : 1 , author : 1 } }"""))
  }

  it should "should render $limit" in {
    toBson(limit(5)) should equal(Document("""{ $limit : 5 }"""))
  }

  it should "should render $skip" in {
    toBson(skip(5)) should equal(Document("""{ $skip : 5 }"""))
  }

  it should "should render $unwind" in {
    toBson(unwind("$sizes")) should equal(Document("""{ $unwind : "$sizes" }"""))
  }

  it should "should render $out" in {
    toBson(out("authors")) should equal(Document("""{ $out : "authors" }"""))
  }

  it should "should render $group" in {
    toBson(group("$customerId")) should equal(Document("""{ $group : { _id : "$customerId" } }"""))
    toBson(group(null)) should equal(Document("""{ $group : { _id : null } }"""))

    toBson(group(Document("""{ month: { $month: "$date" }, day: { $dayOfMonth: "$date" }, year: { $year: "$date" } }"""))) should equal(
      Document("""{ $group : { _id : { month: { $month: "$date" }, day: { $dayOfMonth: "$date" }, year: { $year: "$date" } } } }""")
    )

    val groupDocument = Document("""{
      $group : {
        _id : null,
        sum: { $sum: { $multiply: [ "$price", "$quantity" ] } },
        avg: { $avg: "$quantity" },
        min: { $min: "$quantity" }
        max: { $max: "$quantity" }
        first: { $first: "$quantity" }
        last: { $last: "$quantity" }
        all: { $push: "$quantity" }
        unique: { $addToSet: "$quantity" }
      }
    }""")

    toBson(group(
      null,
      sum("sum", Document("""{ $multiply: [ "$price", "$quantity" ] }""")),
      avg("avg", "$quantity"),
      min("min", "$quantity"),
      max("max", "$quantity"),
      first("first", "$quantity"),
      last("last", "$quantity"),
      push("all", "$quantity"),
      addToSet("unique", "$quantity")
    )) should equal(groupDocument)
  }

}
