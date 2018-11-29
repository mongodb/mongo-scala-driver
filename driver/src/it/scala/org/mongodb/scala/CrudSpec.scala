/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala

import java.io.File

import scala.collection.JavaConverters._
import scala.io.Source

import org.bson.BsonValue
import com.mongodb.client.model._

import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonInt32, BsonNull, BsonString}
import org.mongodb.scala.result.UpdateResult
import org.scalatest.Inspectors.forEvery

class CrudSpec extends RequiresMongoDBISpec with FuturesSpec {
  lazy val readTests = new File(getClass.getResource("/crud/read").toURI).listFiles
  lazy val writeTests = new File(getClass.getResource("/crud/write").toURI).listFiles
  lazy val files = (readTests ++ writeTests).filter(_.getName.endsWith(".json"))
  var collection: Option[MongoCollection[BsonDocument]] = None

  forEvery (files) { (file: File) =>
    s"Running ${file.getName} tests" should "pass all scenarios" in withDatabase(databaseName) {
      database =>

        collection = Some(database.getCollection(collectionName))

        val definition = BsonDocument(Source.fromFile(file).getLines.mkString)
        val data = definition.getArray("data").asScala.map(_.asDocument())
        val tests = definition.getArray("tests").asScala.map(_.asDocument())

        if (serverAtLeastMinVersion(definition) && serverLessThanMaxVersion(definition)) {
          forEvery(tests) { (test: BsonDocument) =>
            val description = test.getString("description").getValue
            val operation: BsonDocument = test.getDocument("operation", BsonDocument())
            val outcome: BsonDocument = test.getDocument("outcome", BsonDocument())
            info(description)

            prepCollection(data)
            val results: BsonValue = runOperation(operation)
            val expectedResults: BsonValue = outcome.get("result")
            results should equal(expectedResults)

            if (outcome.containsKey("collection")) {
              val collectionData = outcome.getDocument("collection")
              val expectedDocuments = collectionData.getArray("data").asScala.map(_.asDocument())
              var coll = collection.get
              if (collectionData.containsKey("name")) {
                coll = database.getCollection[BsonDocument](collectionData.getString("name").getValue)
              }
              expectedDocuments should contain theSameElementsInOrderAs coll.find[BsonDocument]().futureValue
            }
          }
        } else {
          info(s"Skipped $file: Server version check failed")
        }
    }
  }

  def collectionValues(database: MongoDatabase, outcome: BsonDocument): (Seq[BsonDocument], Seq[BsonDocument]) = {
    val collectionData = outcome.getDocument("collection")
    val expectedDocuments = collectionData.getArray("data").asScala.map(_.asDocument())
    val coll = if (collectionData.containsKey("name")) {
      database.getCollection[BsonDocument](collectionData.getString("name").getValue)
    } else {
      collection.get
    }
    (expectedDocuments, coll.find[BsonDocument]().futureValue)
  }

  private def prepCollection(data: Seq[BsonDocument]): Unit = {
      collection.get.drop().futureValue
      collection.get.insertMany(data).futureValue
  }

  // scalastyle:off cyclomatic.complexity
  private def runOperation(operation: BsonDocument): BsonValue = {
    val op = operation.getString("name").getValue match {
        case "aggregate" => doAggregation _
        case "count" => doCount _
        case "distinct" => doDistinct _
        case "find" => doFind _
        case "deleteMany" => doDeleteMany _
        case "deleteOne" => doDeleteOne _
        case "findOneAndDelete" => doFindOneAndDelete _
        case "findOneAndReplace" => doFindOneAndReplace _
        case "findOneAndUpdate" => doFindOneAndUpdate _
        case "insertMany" => doInsertMany _
        case "insertOne" => doInsertOne _
        case "replaceOne" => doReplaceOne _
        case "updateMany" => doUpdateMany _
        case "updateOne" => doUpdateOne _
        case x => (args: BsonDocument) => throw new IllegalArgumentException(s"Unknown operation: $x")
    }
    op(operation.getDocument("arguments"))
  }
  // scalastyle:on cyclomatic.complexity

  private def doAggregation(arguments: BsonDocument): BsonValue = {
    val pipeline = arguments.getArray("pipeline").asScala.map(_.asDocument())
    val observable = collection.get.aggregate[BsonDocument](pipeline)
    if (arguments.containsKey("collation")) observable.collation(getCollation(arguments.getDocument("collation")))
    BsonArray(observable.futureValue)
  }

  private def doCount(arguments: BsonDocument): BsonValue = {
    val options: CountOptions = new CountOptions
    if (arguments.containsKey("skip")) options.skip(arguments.getNumber("skip").intValue)
    if (arguments.containsKey("limit")) options.limit(arguments.getNumber("limit").intValue)
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    BsonInt32(collection.get.countDocuments(arguments.getDocument("filter"), options).futureValue.toInt)
  }

  private def doDistinct(arguments: BsonDocument): BsonValue = {
    val observable = collection.get.distinct[BsonValue](arguments.getString("fieldName").getValue)
    if (arguments.containsKey("filter")) observable.filter(arguments.getDocument("filter"))
    if (arguments.containsKey("collation")) observable.collation(getCollation(arguments.getDocument("collation")))
    BsonArray(observable.futureValue)
  }

  private def doFind(arguments: BsonDocument): BsonValue = {
    val observable = collection.get.find[BsonDocument](arguments.getDocument("filter"))
    if (arguments.containsKey("skip")) observable.skip(arguments.getNumber("skip").intValue)
    if (arguments.containsKey("limit")) observable.limit(arguments.getNumber("limit").intValue)
    if (arguments.containsKey("collation")) observable.collation(getCollation(arguments.getDocument("collation")))
    BsonArray(observable.futureValue)
  }

  private def doDeleteMany(arguments: BsonDocument): BsonValue = {
    val options: DeleteOptions = new DeleteOptions
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    val result = collection.get.deleteMany(arguments.getDocument("filter"), options).futureValue
    new BsonDocument("deletedCount", BsonInt32(result.getDeletedCount.toInt))
  }

  private def doDeleteOne(arguments: BsonDocument): BsonValue = {
    val options: DeleteOptions = new DeleteOptions
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    val result = collection.get.deleteOne(arguments.getDocument("filter"), options).futureValue
    new BsonDocument("deletedCount", BsonInt32(result.getDeletedCount.toInt))
  }

  private def doFindOneAndDelete(arguments: BsonDocument): BsonValue = {
    val options: FindOneAndDeleteOptions = new FindOneAndDeleteOptions
    if (arguments.containsKey("projection")) options.projection(arguments.getDocument("projection"))
    if (arguments.containsKey("sort")) options.sort(arguments.getDocument("sort"))
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    Option(collection.get.findOneAndDelete(arguments.getDocument("filter"), options).futureValue).getOrElse(BsonNull())
  }

  private def doFindOneAndReplace(arguments: BsonDocument): BsonValue = {
    val options: FindOneAndReplaceOptions = new FindOneAndReplaceOptions
    if (arguments.containsKey("projection")) options.projection(arguments.getDocument("projection"))
    if (arguments.containsKey("sort")) options.sort(arguments.getDocument("sort"))
    if (arguments.containsKey("upsert")) options.upsert(arguments.getBoolean("upsert").getValue)
    if (arguments.containsKey("returnDocument")) {
      val rd = if (arguments.getString("returnDocument").getValue == "After") ReturnDocument.AFTER else ReturnDocument.BEFORE
      options.returnDocument(rd)
    }
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    Option(collection.get.findOneAndReplace(arguments.getDocument("filter"),
      arguments.getDocument("replacement"), options).futureValue).getOrElse(BsonNull())
  }

  private def doFindOneAndUpdate(arguments: BsonDocument): BsonValue = {
    val options: FindOneAndUpdateOptions = new FindOneAndUpdateOptions
    if (arguments.containsKey("projection")) options.projection(arguments.getDocument("projection"))
    if (arguments.containsKey("sort")) options.sort(arguments.getDocument("sort"))
    if (arguments.containsKey("upsert")) options.upsert(arguments.getBoolean("upsert").getValue)
    if (arguments.containsKey("returnDocument")) {
      val rd = if (arguments.getString("returnDocument").getValue == "After") ReturnDocument.AFTER else ReturnDocument.BEFORE
      options.returnDocument(rd)
    }
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    Option(collection.get.findOneAndUpdate(arguments.getDocument("filter"),
      arguments.getDocument("update"), options).futureValue).getOrElse(BsonNull())
  }

  private def doInsertOne(arguments: BsonDocument): BsonValue = {
    val document = arguments.getDocument("document")
    collection.get.insertOne(document).futureValue
    Document(("insertedId", Option(document.get("_id")).getOrElse(BsonNull()))).underlying
  }

  private def doInsertMany(arguments: BsonDocument): BsonValue = {
    val documents = arguments.getArray("documents").asScala.map(_.asDocument())
    collection.get.insertMany(documents).futureValue
    Document(("insertedIds", documents.map(doc => Option(doc.get("_id")).getOrElse(BsonNull())))).underlying
  }

  private def doReplaceOne(arguments: BsonDocument): BsonValue = {
    val options: UpdateOptions = new UpdateOptions
    if (arguments.containsKey("upsert")) options.upsert(arguments.getBoolean("upsert").getValue)
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    val rawResult = collection.get.replaceOne(arguments.getDocument("filter"), arguments.getDocument("replacement"), options).futureValue
    convertUpdateResult(rawResult)
  }

  private def doUpdateMany(arguments: BsonDocument): BsonValue = {
    val options: UpdateOptions = new UpdateOptions
    if (arguments.containsKey("upsert")) options.upsert(arguments.getBoolean("upsert").getValue)
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    val result = collection.get.updateMany(arguments.getDocument("filter"),
      arguments.getDocument("update"), options).futureValue
    convertUpdateResult(result)
  }

  private def doUpdateOne(arguments: BsonDocument): BsonValue = {
    val options: UpdateOptions = new UpdateOptions
    if (arguments.containsKey("upsert")) options.upsert(arguments.getBoolean("upsert").getValue)
    if (arguments.containsKey("collation")) options.collation(getCollation(arguments.getDocument("collation")))
    val result = collection.get.updateOne(arguments.getDocument("filter"), arguments.getDocument("update"), options).futureValue
    convertUpdateResult(result)
  }

  private def getCollation(bsonCollation: BsonDocument): Collation = {
    val builder: Collation.Builder = Collation.builder
    if (bsonCollation.containsKey("locale")) builder.locale(bsonCollation.getString("locale").getValue)
    if (bsonCollation.containsKey("caseLevel")) builder.caseLevel(bsonCollation.getBoolean("caseLevel").getValue)
    if (bsonCollation.containsKey("caseFirst")) builder.collationCaseFirst(CollationCaseFirst.fromString(bsonCollation.getString("caseFirst").getValue))
    if (bsonCollation.containsKey("numericOrdering")) builder.numericOrdering(bsonCollation.getBoolean("numericOrdering").getValue)
    if (bsonCollation.containsKey("strength")) builder.collationStrength(CollationStrength.fromInt(bsonCollation.getInt32("strength").getValue))
    if (bsonCollation.containsKey("alternate")) builder.collationAlternate(CollationAlternate.fromString(bsonCollation.getString("alternate").getValue))
    if (bsonCollation.containsKey("maxVariable")) builder.collationMaxVariable(CollationMaxVariable.fromString(bsonCollation.getString("maxVariable").getValue))
    if (bsonCollation.containsKey("normalization")) builder.normalization(bsonCollation.getBoolean("normalization").getValue)
    if (bsonCollation.containsKey("backwards")) builder.backwards(bsonCollation.getBoolean("backwards").getValue)
    builder.build
  }

  private def convertUpdateResult(result: UpdateResult): BsonDocument = {
    val resultDoc: BsonDocument = new BsonDocument("matchedCount", new BsonInt32(result.getMatchedCount.toInt))
    if (result.isModifiedCountAvailable) {
      resultDoc.append("modifiedCount", new BsonInt32(result.getModifiedCount.toInt))
    }

    val upsertedCount = result.getUpsertedId match {
      case id: BsonValue if !id.isObjectId => resultDoc.append("upsertedId", id); new BsonInt32(1)
      case _: BsonValue  => new BsonInt32(1)
      case _ /* empty */ => new BsonInt32(0)
    }
    resultDoc.append("upsertedCount", upsertedCount)
  }



  private def serverAtLeastMinVersion(definition: Document): Boolean = {
    definition.get[BsonString]("minServerVersion") match {
      case Some(minServerVersion) =>
        serverVersionAtLeast(minServerVersion.getValue.split("\\.").map(_.toInt).padTo(3, 0).take(3).toList)
      case None => true
    }
  }

  private def serverLessThanMaxVersion(definition: Document): Boolean = {
    definition.get[BsonString]("maxServerVersion") match {
      case Some(maxServerVersion) =>
        serverVersionLessThan(maxServerVersion.getValue.split("\\.").map(_.toInt).padTo(3, 0).take(3).toList)
      case None => true
    }
  }

}
