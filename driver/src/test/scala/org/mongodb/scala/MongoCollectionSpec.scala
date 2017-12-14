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

package org.mongodb.scala

import scala.collection.JavaConverters._

import org.bson.BsonDocument
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import com.mongodb.async.client.{MongoCollection => JMongoCollection}
import com.mongodb.client.model.CountOptions

import org.mongodb.scala.model._
import org.mongodb.scala.result._
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class MongoCollectionSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoCollection[Document]]
  val clientSession = mock[ClientSession]
  val mongoCollection = MongoCollection[Document](wrapped)
  val readPreference = ReadPreference.secondary()
  val collation = Collation.builder().locale("en").build()

  val filter = Document("filter" -> 1)
  def observer[T] = new Observer[T]() {
    override def onError(throwable: Throwable): Unit = {}
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
    override def onComplete(): Unit = {}
    override def onNext(doc: T): Unit = {}
  }

  "MongoCollection" should "have the same methods as the wrapped MongoCollection" in {
    val wrapped = classOf[JMongoCollection[Document]].getMethods.map(_.getName).toSet
    val local = classOf[MongoCollection[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "return the underlying getNamespace" in {
    wrapped.expects('getNamespace)().once()

    mongoCollection.namespace
  }

  it should "return the underlying getCodecRegistry" in {
    wrapped.expects('getCodecRegistry)().once()

    mongoCollection.codecRegistry
  }

  it should "return the underlying getReadPreference" in {
    wrapped.expects('getReadPreference)().once()

    mongoCollection.readPreference
  }

  it should "return the underlying getWriteConcern" in {
    wrapped.expects('getWriteConcern)().once()

    mongoCollection.writeConcern
  }

  it should "return the underlying getReadConcern" in {
    wrapped.expects('getReadConcern)().once()

    mongoCollection.readConcern
  }

  it should "return the underlying getDocumentClass" in {
    wrapped.expects('getDocumentClass)().once()

    mongoCollection.documentClass
  }

  it should "return the underlying withCodecRegistry" in {
    val codecRegistry = fromProviders(new BsonValueCodecProvider())

    wrapped.expects('withCodecRegistry)(codecRegistry).once()

    mongoCollection.withCodecRegistry(codecRegistry)
  }

  it should "return the underlying withReadPreference" in {
    wrapped.expects('withReadPreference)(readPreference).once()

    mongoCollection.withReadPreference(readPreference)
  }

  it should "return the underlying withWriteConcern" in {
    val writeConcern = WriteConcern.MAJORITY
    wrapped.expects('withWriteConcern)(writeConcern).once()

    mongoCollection.withWriteConcern(writeConcern)
  }

  it should "return the underlying withReadConcern" in {
    val readConcern = ReadConcern.MAJORITY
    wrapped.expects('withReadConcern)(readConcern).once()

    mongoCollection.withReadConcern(readConcern)
  }

  it should "return the underlying withDocumentClass" in {
    wrapped.expects('withDocumentClass)(classOf[Document]).once()
    wrapped.expects('withDocumentClass)(classOf[Document]).once()
    wrapped.expects('withDocumentClass)(classOf[BsonDocument]).once()

    mongoCollection.withDocumentClass()
    mongoCollection.withDocumentClass[Document]()
    mongoCollection.withDocumentClass[BsonDocument]()
  }

  it should "return the underlying count" in {
    val countOptions = new CountOptions().hintString("Hint")

    wrapped.expects('count)(*).once()
    wrapped.expects('count)(filter, *).once()
    wrapped.expects('count)(filter, countOptions, *).once()
    wrapped.expects('count)(clientSession, *).once()
    wrapped.expects('count)(clientSession, filter, *).once()
    wrapped.expects('count)(clientSession, filter, countOptions, *).once()

    mongoCollection.count().subscribe(observer[Long])
    mongoCollection.count(filter).subscribe(observer[Long])
    mongoCollection.count(filter, countOptions).subscribe(observer[Long])
    mongoCollection.count(clientSession).subscribe(observer[Long])
    mongoCollection.count(clientSession, filter).subscribe(observer[Long])
    mongoCollection.count(clientSession, filter, countOptions).subscribe(observer[Long])
  }

  it should "wrap the underlying DistinctObservable correctly" in {
    wrapped.expects('distinct)("fieldName", classOf[String]).once()
    wrapped.expects('distinct)("fieldName", filter, classOf[String]).once()
    wrapped.expects('distinct)(clientSession, "fieldName", classOf[String]).once()
    wrapped.expects('distinct)(clientSession, "fieldName", filter, classOf[String]).once()

    mongoCollection.distinct[String]("fieldName")
    mongoCollection.distinct[String]("fieldName", filter)
    mongoCollection.distinct[String](clientSession, "fieldName")
    mongoCollection.distinct[String](clientSession, "fieldName", filter)
  }

  it should "wrap the underlying FindObservable correctly" in {
    wrapped.expects('find)(classOf[Document]).once()
    wrapped.expects('find)(classOf[BsonDocument]).once()
    wrapped.expects('find)(filter, classOf[Document]).once()
    wrapped.expects('find)(filter, classOf[BsonDocument]).once()
    wrapped.expects('find)(clientSession, classOf[Document]).once()
    wrapped.expects('find)(clientSession, classOf[BsonDocument]).once()
    wrapped.expects('find)(clientSession, filter, classOf[Document]).once()
    wrapped.expects('find)(clientSession, filter, classOf[BsonDocument]).once()

    mongoCollection.find() shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument]() shouldBe a[FindObservable[_]]
    mongoCollection.find(filter) shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument](filter) shouldBe a[FindObservable[_]]
    mongoCollection.find(clientSession) shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument](clientSession) shouldBe a[FindObservable[_]]
    mongoCollection.find(clientSession, filter) shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument](clientSession, filter) shouldBe a[FindObservable[_]]
  }

  it should "wrap the underlying AggregateObservable correctly" in {
    val pipeline = List(Document("$match" -> 1))

    wrapped.expects('aggregate)(pipeline.asJava, classOf[Document]).once()
    wrapped.expects('aggregate)(pipeline.asJava, classOf[BsonDocument]).once()
    wrapped.expects('aggregate)(clientSession, pipeline.asJava, classOf[Document]).once()
    wrapped.expects('aggregate)(clientSession, pipeline.asJava, classOf[BsonDocument]).once()

    mongoCollection.aggregate(pipeline) shouldBe a[AggregateObservable[_]]
    mongoCollection.aggregate[BsonDocument](pipeline) shouldBe a[AggregateObservable[_]]
    mongoCollection.aggregate(clientSession, pipeline) shouldBe a[AggregateObservable[_]]
    mongoCollection.aggregate[BsonDocument](clientSession, pipeline) shouldBe a[AggregateObservable[_]]
  }

  it should "wrap the underlying MapReduceObservable correctly" in {
    wrapped.expects('mapReduce)("map", "reduce", classOf[Document]).once()
    wrapped.expects('mapReduce)("map", "reduce", classOf[BsonDocument]).once()
    wrapped.expects('mapReduce)(clientSession, "map", "reduce", classOf[Document]).once()
    wrapped.expects('mapReduce)(clientSession, "map", "reduce", classOf[BsonDocument]).once()

    mongoCollection.mapReduce("map", "reduce") shouldBe a[MapReduceObservable[_]]
    mongoCollection.mapReduce[BsonDocument]("map", "reduce") shouldBe a[MapReduceObservable[_]]
    mongoCollection.mapReduce(clientSession, "map", "reduce") shouldBe a[MapReduceObservable[_]]
    mongoCollection.mapReduce[BsonDocument](clientSession, "map", "reduce") shouldBe a[MapReduceObservable[_]]
  }

  it should "wrap the underlying bulkWrite correctly" in {
    val bulkRequests = List(
      InsertOneModel(Document("a" -> 1)),
      DeleteOneModel(filter),
      UpdateOneModel(filter, Document("$set" -> Document("b" -> 1)))
    )
    val bulkWriteOptions = new BulkWriteOptions().ordered(true)

    wrapped.expects('bulkWrite)(bulkRequests.asJava, *).once()
    wrapped.expects('bulkWrite)(bulkRequests.asJava, bulkWriteOptions, *).once()
    wrapped.expects('bulkWrite)(clientSession, bulkRequests.asJava, *).once()
    wrapped.expects('bulkWrite)(clientSession, bulkRequests.asJava, bulkWriteOptions, *).once()

    mongoCollection.bulkWrite(bulkRequests).subscribe(observer[BulkWriteResult])
    mongoCollection.bulkWrite(bulkRequests, bulkWriteOptions).subscribe(observer[BulkWriteResult])
    mongoCollection.bulkWrite(clientSession, bulkRequests).subscribe(observer[BulkWriteResult])
    mongoCollection.bulkWrite(clientSession, bulkRequests, bulkWriteOptions).subscribe(observer[BulkWriteResult])
  }

  it should "wrap the underlying insertOne correctly" in {
    val insertDoc = Document("a" -> 1)
    val insertOptions = InsertOneOptions().bypassDocumentValidation(true)
    wrapped.expects('insertOne)(insertDoc, *).once()
    wrapped.expects('insertOne)(insertDoc, insertOptions, *).once()
    wrapped.expects('insertOne)(clientSession, insertDoc, *).once()
    wrapped.expects('insertOne)(clientSession, insertDoc, insertOptions, *).once()

    mongoCollection.insertOne(insertDoc).subscribe(observer[Completed])
    mongoCollection.insertOne(insertDoc, insertOptions).subscribe(observer[Completed])
    mongoCollection.insertOne(clientSession, insertDoc).subscribe(observer[Completed])
    mongoCollection.insertOne(clientSession, insertDoc, insertOptions).subscribe(observer[Completed])
  }

  it should "wrap the underlying insertMany correctly" in {
    val insertDocs = List(Document("a" -> 1))
    val insertOptions = new InsertManyOptions().ordered(false)

    wrapped.expects('insertMany)(insertDocs.asJava, *).once()
    wrapped.expects('insertMany)(insertDocs.asJava, insertOptions, *).once()
    wrapped.expects('insertMany)(clientSession, insertDocs.asJava, *).once()
    wrapped.expects('insertMany)(clientSession, insertDocs.asJava, insertOptions, *).once()

    mongoCollection.insertMany(insertDocs).subscribe(observer[Completed])
    mongoCollection.insertMany(insertDocs, insertOptions).subscribe(observer[Completed])
    mongoCollection.insertMany(clientSession, insertDocs).subscribe(observer[Completed])
    mongoCollection.insertMany(clientSession, insertDocs, insertOptions).subscribe(observer[Completed])
  }

  it should "wrap the underlying deleteOne correctly" in {
    val options = new DeleteOptions().collation(collation)
    wrapped.expects('deleteOne)(filter, *).once()
    wrapped.expects('deleteOne)(filter, options, *).once()
    wrapped.expects('deleteOne)(clientSession, filter, *).once()
    wrapped.expects('deleteOne)(clientSession, filter, options, *).once()

    mongoCollection.deleteOne(filter).subscribe(observer[DeleteResult])
    mongoCollection.deleteOne(filter, options).subscribe(observer[DeleteResult])
    mongoCollection.deleteOne(clientSession, filter).subscribe(observer[DeleteResult])
    mongoCollection.deleteOne(clientSession, filter, options).subscribe(observer[DeleteResult])
  }

  it should "wrap the underlying deleteMany correctly" in {
    val options = new DeleteOptions().collation(collation)
    wrapped.expects('deleteMany)(filter, *).once()
    wrapped.expects('deleteMany)(filter, options, *).once()
    wrapped.expects('deleteMany)(clientSession, filter, *).once()
    wrapped.expects('deleteMany)(clientSession, filter, options, *).once()

    mongoCollection.deleteMany(filter).subscribe(observer[DeleteResult])
    mongoCollection.deleteMany(filter, options).subscribe(observer[DeleteResult])
    mongoCollection.deleteMany(clientSession, filter).subscribe(observer[DeleteResult])
    mongoCollection.deleteMany(clientSession, filter, options).subscribe(observer[DeleteResult])
  }

  it should "wrap the underlying replaceOne correctly" in {
    val replacement = Document("a" -> 1)
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('replaceOne)(filter, replacement, *).once()
    wrapped.expects('replaceOne)(filter, replacement, updateOptions, *).once()
    wrapped.expects('replaceOne)(clientSession, filter, replacement, *).once()
    wrapped.expects('replaceOne)(clientSession, filter, replacement, updateOptions, *).once()

    mongoCollection.replaceOne(filter, replacement).subscribe(observer[UpdateResult])
    mongoCollection.replaceOne(filter, replacement, updateOptions).subscribe(observer[UpdateResult])
    mongoCollection.replaceOne(clientSession, filter, replacement).subscribe(observer[UpdateResult])
    mongoCollection.replaceOne(clientSession, filter, replacement, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying updateOne correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('updateOne)(filter, update, *).once()
    wrapped.expects('updateOne)(filter, update, updateOptions, *).once()
    wrapped.expects('updateOne)(clientSession, filter, update, *).once()
    wrapped.expects('updateOne)(clientSession, filter, update, updateOptions, *).once()

    mongoCollection.updateOne(filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateOne(filter, update, updateOptions).subscribe(observer[UpdateResult])
    mongoCollection.updateOne(clientSession, filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateOne(clientSession, filter, update, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying updateMany correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('updateMany)(filter, update, *).once()
    wrapped.expects('updateMany)(filter, update, updateOptions, *).once()
    wrapped.expects('updateMany)(clientSession, filter, update, *).once()
    wrapped.expects('updateMany)(clientSession, filter, update, updateOptions, *).once()

    mongoCollection.updateMany(filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateMany(filter, update, updateOptions).subscribe(observer[UpdateResult])
    mongoCollection.updateMany(clientSession, filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateMany(clientSession, filter, update, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying findOneAndDelete correctly" in {
    val options = new FindOneAndDeleteOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndDelete)(filter, *).once()
    wrapped.expects('findOneAndDelete)(filter, options, *).once()
    wrapped.expects('findOneAndDelete)(clientSession, filter, *).once()
    wrapped.expects('findOneAndDelete)(clientSession, filter, options, *).once()

    mongoCollection.findOneAndDelete(filter).subscribe(observer[Document])
    mongoCollection.findOneAndDelete(filter, options).subscribe(observer[Document])
    mongoCollection.findOneAndDelete(clientSession, filter).subscribe(observer[Document])
    mongoCollection.findOneAndDelete(clientSession, filter, options).subscribe(observer[Document])
  }

  it should "wrap the underlying findOneAndReplace correctly" in {
    val replacement = Document("a" -> 2)
    val options = new FindOneAndReplaceOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndReplace)(filter, replacement, *).once()
    wrapped.expects('findOneAndReplace)(filter, replacement, options, *).once()
    wrapped.expects('findOneAndReplace)(clientSession, filter, replacement, *).once()
    wrapped.expects('findOneAndReplace)(clientSession, filter, replacement, options, *).once()

    mongoCollection.findOneAndReplace(filter, replacement).subscribe(observer[Document])
    mongoCollection.findOneAndReplace(filter, replacement, options).subscribe(observer[Document])
    mongoCollection.findOneAndReplace(clientSession, filter, replacement).subscribe(observer[Document])
    mongoCollection.findOneAndReplace(clientSession, filter, replacement, options).subscribe(observer[Document])
  }

  it should "wrap the underlying findOneAndUpdate correctly" in {
    val update = Document("a" -> 2)
    val options = new FindOneAndUpdateOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndUpdate)(filter, update, *).once()
    wrapped.expects('findOneAndUpdate)(filter, update, options, *).once()
    wrapped.expects('findOneAndUpdate)(clientSession, filter, update, *).once()
    wrapped.expects('findOneAndUpdate)(clientSession, filter, update, options, *).once()

    mongoCollection.findOneAndUpdate(filter, update).subscribe(observer[Document])
    mongoCollection.findOneAndUpdate(filter, update, options).subscribe(observer[Document])
    mongoCollection.findOneAndUpdate(clientSession, filter, update).subscribe(observer[Document])
    mongoCollection.findOneAndUpdate(clientSession, filter, update, options).subscribe(observer[Document])
  }

  it should "wrap the underlying drop correctly" in {
    wrapped.expects('drop)(*).once()
    wrapped.expects('drop)(clientSession, *).once()

    mongoCollection.drop().subscribe(observer[Completed])
    mongoCollection.drop(clientSession).subscribe(observer[Completed])
  }

  it should "wrap the underlying createIndex correctly" in {
    val index = Document("a" -> 1)
    val options = new IndexOptions().background(true)

    wrapped.expects('createIndex)(index, *).once()
    wrapped.expects('createIndex)(index, options, *).once()
    wrapped.expects('createIndex)(clientSession, index, *).once()
    wrapped.expects('createIndex)(clientSession, index, options, *).once()

    mongoCollection.createIndex(index).subscribe(observer[String])
    mongoCollection.createIndex(index, options).subscribe(observer[String])
    mongoCollection.createIndex(clientSession, index).subscribe(observer[String])
    mongoCollection.createIndex(clientSession, index, options).subscribe(observer[String])
  }

  it should "wrap the underlying createIndexes correctly" in {
    val indexes = new IndexModel(Document("a" -> 1))
    val options = new CreateIndexOptions()

    // https://github.com/paulbutcher/ScalaMock/issues/93
    wrapped.expects('createIndexes)(List(indexes).asJava, *).once()
    wrapped.expects('createIndexes)(List(indexes).asJava, options, *).once()
    wrapped.expects('createIndexes)(clientSession, List(indexes).asJava, *).once()
    wrapped.expects('createIndexes)(clientSession, List(indexes).asJava, options, *).once()

    mongoCollection.createIndexes(List(indexes)).subscribe(observer[String])
    mongoCollection.createIndexes(List(indexes), options).subscribe(observer[String])
    mongoCollection.createIndexes(clientSession, List(indexes)).subscribe(observer[String])
    mongoCollection.createIndexes(clientSession, List(indexes), options).subscribe(observer[String])
  }

  it should "wrap the underlying listIndexes correctly" in {
    wrapped.expects('listIndexes)(classOf[Document]).once()
    wrapped.expects('listIndexes)(classOf[BsonDocument]).once()
    wrapped.expects('listIndexes)(clientSession, classOf[Document]).once()
    wrapped.expects('listIndexes)(clientSession, classOf[BsonDocument]).once()

    mongoCollection.listIndexes()
    mongoCollection.listIndexes[BsonDocument]()
    mongoCollection.listIndexes(clientSession)
    mongoCollection.listIndexes[BsonDocument](clientSession)
  }

  it should "wrap the underlying dropIndex correctly" in {
    val indexDocument = Document("""{a: 1}""")
    val options = new DropIndexOptions()
    wrapped.expects('dropIndex)("indexName", *).once()
    wrapped.expects('dropIndex)(indexDocument, *).once()
    wrapped.expects('dropIndex)("indexName", options, *).once()
    wrapped.expects('dropIndex)(indexDocument, options, *).once()
    wrapped.expects('dropIndex)(clientSession, "indexName", *).once()
    wrapped.expects('dropIndex)(clientSession, indexDocument, *).once()
    wrapped.expects('dropIndex)(clientSession, "indexName", options, *).once()
    wrapped.expects('dropIndex)(clientSession, indexDocument, options, *).once()

    mongoCollection.dropIndex("indexName").subscribe(observer[Completed])
    mongoCollection.dropIndex(indexDocument).subscribe(observer[Completed])
    mongoCollection.dropIndex("indexName", options).subscribe(observer[Completed])
    mongoCollection.dropIndex(indexDocument, options).subscribe(observer[Completed])
    mongoCollection.dropIndex(clientSession, "indexName").subscribe(observer[Completed])
    mongoCollection.dropIndex(clientSession, indexDocument).subscribe(observer[Completed])
    mongoCollection.dropIndex(clientSession, "indexName", options).subscribe(observer[Completed])
    mongoCollection.dropIndex(clientSession, indexDocument, options).subscribe(observer[Completed])
  }

  it should "wrap the underlying dropIndexes correctly" in {

    val options = new DropIndexOptions()
    wrapped.expects('dropIndexes)(*).once()
    wrapped.expects('dropIndexes)(options, *).once()
    wrapped.expects('dropIndexes)(clientSession, *).once()
    wrapped.expects('dropIndexes)(clientSession, options, *).once()

    mongoCollection.dropIndexes().subscribe(observer[Completed])
    mongoCollection.dropIndexes(options).subscribe(observer[Completed])
    mongoCollection.dropIndexes(clientSession).subscribe(observer[Completed])
    mongoCollection.dropIndexes(clientSession, options).subscribe(observer[Completed])
  }

  it should "wrap the underlying renameCollection correctly" in {
    val newNamespace = new MongoNamespace("db", "coll")
    val options = new RenameCollectionOptions()

    wrapped.expects('renameCollection)(newNamespace, *).once()
    wrapped.expects('renameCollection)(newNamespace, options, *).once()
    wrapped.expects('renameCollection)(clientSession, newNamespace, *).once()
    wrapped.expects('renameCollection)(clientSession, newNamespace, options, *).once()

    mongoCollection.renameCollection(newNamespace).subscribe(observer[Completed])
    mongoCollection.renameCollection(newNamespace, options).subscribe(observer[Completed])
    mongoCollection.renameCollection(clientSession, newNamespace).subscribe(observer[Completed])
    mongoCollection.renameCollection(clientSession, newNamespace, options).subscribe(observer[Completed])
  }

  it should "wrap the underlying ChangeStreamIterable correctly" in {
    val pipeline = List(Document("$match" -> 1))

    wrapped.expects('watch)(classOf[Document]).once()
    wrapped.expects('watch)(classOf[BsonDocument]).once()
    wrapped.expects('watch)(pipeline.asJava, classOf[Document]).once()
    wrapped.expects('watch)(pipeline.asJava, classOf[BsonDocument]).once()
    wrapped.expects('watch)(clientSession, classOf[Document]).once()
    wrapped.expects('watch)(clientSession, classOf[BsonDocument]).once()
    wrapped.expects('watch)(clientSession, pipeline.asJava, classOf[Document]).once()
    wrapped.expects('watch)(clientSession, pipeline.asJava, classOf[BsonDocument]).once()

    mongoCollection.watch() shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch[BsonDocument]() shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch(pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch[BsonDocument](pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch(clientSession) shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch[BsonDocument](clientSession) shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch(clientSession, pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoCollection.watch[BsonDocument](clientSession, pipeline) shouldBe a[ChangeStreamObservable[_]]
  }

}
