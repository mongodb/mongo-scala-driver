/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.scala.reactivestreams.client

import com.mongodb.MongoNamespace
import org.bson.Document

import scala.language.implicitConversions

class SmokeTestISpec extends RequiresMongoDBISpec {

  "The Scala driver" should "handle common scenarios without error" in withDatabase(databaseName) {
    database =>
      val client = mongoClient()
      val document = new Document("_id", 1)
      def updatedDocument = new Document("_id", 1).append("a", 1)

      val names = client.listDatabaseNames().futureValue

      info("Creating a collection")
      println(database.name)
      database.createCollection(collectionName).futureValue shouldBe List(null)

      info("Database names should include the new collection")
      val updatedNames = client.listDatabaseNames().futureValue
      updatedNames should contain(databaseName)
      updatedNames.length shouldBe names.length + 1

      info("The collection name should be in the collection names list")
      val collectionNames = database.listCollectionNames().futureValue
      collectionNames should contain(collectionName)

      info("The collection should be empty")
      val collection = database.getCollection(collectionName)
      collection.count().futureValue.head shouldBe 0

      info("find first should return null if no documents")
      collection.find().first().futureValue.head shouldBe null // This should be an option

      info("Insert a document")
      collection.insertOne(document).futureValue.head shouldBe null

      info("The count should be one")
      collection.count().futureValue.head shouldBe 1

      info("the find that document")
      collection.find().futureValue.head shouldBe document

      info("update that document")
      collection.updateOne(document, new Document("$set", new Document("a", 1))).futureValue.head.wasAcknowledged shouldBe true

      info("the find the updated document")
      collection.find().first().futureValue.head shouldBe updatedDocument

      info("aggregate the collection")
      collection.aggregate(List(new Document("$match", new Document("a", 1)))).futureValue.head shouldBe updatedDocument

      info("remove all documents")
      collection.deleteOne(new Document()).futureValue.head.getDeletedCount() shouldBe 1

      info("The count is zero")
      collection.count().futureValue.head shouldBe 0

      info("create an index")
      collection.createIndex(new Document("test", 1)).futureValue.head shouldBe null

      info("has the newly created index")
      val indexNames = collection.listIndexes().futureValue.map(doc => doc.getString("name"))
      indexNames should contain("_id_")
      indexNames should contain("test_1")

      info("drop the index")
      collection.dropIndex("test_1").futureValue.head shouldBe null

      info("has a single index left '_id'")
      collection.listIndexes.futureValue.length shouldBe 1

      info("can rename the collection")
      val newCollectionName = "new" + collectionName.capitalize
      collection.renameCollection(new MongoNamespace(databaseName, newCollectionName)).futureValue.head shouldBe null

      info("the new collection name is in the collection names list")
      database.listCollectionNames().futureValue should contain(newCollectionName)

      info("drop the new collection")
      val newCollection = database.getCollection(newCollectionName)
      newCollection.dropCollection().futureValue.head shouldBe null

      info("there are no indexes")
      newCollection.listIndexes().futureValue.length shouldBe 0

      info("the new collection name is no longer in the collection names list")
      database.listCollectionNames().futureValue should not contain (newCollectionName)
  }
}
