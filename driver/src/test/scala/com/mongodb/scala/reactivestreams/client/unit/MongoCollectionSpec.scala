package com.mongodb.scala.reactivestreams.client

import com.mongodb.client.model._
import com.mongodb.reactivestreams.client.{ MongoCollection => JMongoCollection }
import com.mongodb.{ MongoNamespace, ReadPreference, WriteConcern }
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.conversions.Bson
import org.bson.{ BsonDocument }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }
import com.mongodb.scala.reactivestreams.client.Implicits._
import com.mongodb.scala.reactivestreams.client.collection.Document
import scala.collection.JavaConverters._

class MongoCollectionSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoCollection[Document]]
  val mongoCollection = MongoCollection(wrapped)
  val readPreference = ReadPreference.secondary()
  val filter = Document("filter" -> 1)

  "MongoCollection" should "have the same methods as the wrapped MongoCollection" in {
    val wrapped = classOf[JMongoCollection[Document]].getMethods.map(_.getName).toSet
    val local = classOf[MongoCollection[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "return the underlying getNamespace" in {
    (wrapped.getNamespace _).expects().once()

    mongoCollection.namespace
  }

  it should "return the underlying getCodecRegistry" in {
    (wrapped.getCodecRegistry _).expects().once()

    mongoCollection.codecRegistry
  }

  it should "return the underlying getReadPreference" in {
    (wrapped.getReadPreference _).expects().once()

    mongoCollection.readPreference
  }

  it should "return the underlying getWriteConcern" in {
    (wrapped.getWriteConcern _).expects().once()

    mongoCollection.writeConcern
  }

  it should "return the underlying getDocumentClass" in {
    (wrapped.getDocumentClass _).expects().once()

    mongoCollection.documentClass
  }

  it should "return the underlying withCodecRegistry" in {
    val codecRegistry = fromProviders(new BsonValueCodecProvider())

    (wrapped.withCodecRegistry _).expects(codecRegistry).once()

    mongoCollection.withCodecRegistry(codecRegistry)
  }

  it should "return the underlying withReadPreference" in {
    (wrapped.withReadPreference _).expects(readPreference).once()

    mongoCollection.withReadPreference(readPreference)
  }

  it should "return the underlying withWriteConcern" in {
    val writeConcern = WriteConcern.MAJORITY
    (wrapped.withWriteConcern _).expects(writeConcern).once()

    mongoCollection.withWriteConcern(writeConcern)
  }

  it should "return the underlying withDocumentClass" in {
    (wrapped.withDocumentClass[Document] _).expects(classOf[Document]).once()
    (wrapped.withDocumentClass[BsonDocument] _).expects(classOf[BsonDocument]).once()

    mongoCollection.withDocumentClass[Document]()
    mongoCollection.withDocumentClass[BsonDocument]()
  }

  it should "return the underlying count" in {
    val countOptions = new CountOptions().hintString("Hint")

    (wrapped.count _).expects().once()
    (wrapped.count(_: Bson)).expects(filter).once()
    (wrapped.count(_: Bson, _: CountOptions)).expects(filter, countOptions).once()

    mongoCollection.count()
    mongoCollection.count(filter)
    mongoCollection.count(filter, countOptions)
  }

  it should "wrap the underlying DistinctPublisher correctly" in {
    (wrapped.distinct[String] _).expects("fieldName", classOf[String]).once()

    mongoCollection.distinct[String]("fieldName") shouldBe a[DistinctPublisher[_]]
  }

  it should "wrap the underlying FindPublisher correctly" in {
    (wrapped.find[Document](_: Class[Document])).expects(classOf[Document]).once()
    (wrapped.find[BsonDocument](_: Class[BsonDocument])).expects(classOf[BsonDocument]).once()

    (wrapped.find[Document](_: Bson, _: Class[Document])).expects(filter, classOf[Document]).once()
    (wrapped.find[BsonDocument](_: Bson, _: Class[BsonDocument])).expects(filter, classOf[BsonDocument]).once()

    mongoCollection.find() shouldBe a[FindPublisher[_]]
    mongoCollection.find[BsonDocument]() shouldBe a[FindPublisher[_]]

    mongoCollection.find(filter) shouldBe a[FindPublisher[_]]
    mongoCollection.find[BsonDocument](filter) shouldBe a[FindPublisher[_]]
  }

  it should "wrap the underlying AggregatePublisher correctly" in {
    val pipeline = List(Document("$match" -> 1))

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/94
    // (wrapped.aggregate[Document](_: util.List[_], _: Class[Document])).expects(pipeline.asJava, classOf[Document]).once()
    // (wrapped.aggregate[BsonDocument](_: util.List[_], _: Class[BsonDocument])).expects(pipeline.asJava, classOf[BsonDocument]).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.aggregate(pipeline) shouldBe a[AggregatePublisher[_]]
    mongoCollection.aggregate[BsonDocument](pipeline) shouldBe a[AggregatePublisher[_]]
  }

  it should "wrap the underlying MapReducePublisher correctly" in {
    (wrapped.mapReduce[Document](_: String, _: String, _: Class[Document])).expects("map", "reduce", classOf[Document]).once()
    (wrapped.mapReduce[BsonDocument](_: String, _: String, _: Class[BsonDocument])).expects("map", "reduce", classOf[BsonDocument]).once()

    mongoCollection.mapReduce("map", "reduce") shouldBe a[MapReducePublisher[_]]
    mongoCollection.mapReduce[BsonDocument]("map", "reduce") shouldBe a[MapReducePublisher[_]]
  }

  it should "wrap the underlying bulkWrite correctly" in {
    val bulkRequests = List(
      new InsertOneModel[Document](Document("a" -> 1)),
      new DeleteOneModel[Document](filter),
      new UpdateOneModel[Document](filter, Document("$set" -> Document("b" -> 1)))
    )
    val bulkWriteOptions = new BulkWriteOptions().ordered(true)

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/94
    // (wrapped.bulkWrite(_: util.List[_ <: WriteModel[_ <: Document]])).expects(bulkWrite.asJava).once()
    // (wrapped.bulkWrite(_: util.List[_ <: WriteModel[_ <: Document]], _: BulkWriteOptions)).expects(bulkWrite.asJava, bulkWriteOptions)
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.bulkWrite(bulkRequests) // Bug with matcher prevents matching against [Publisher[_]]
    mongoCollection.bulkWrite(bulkRequests, bulkWriteOptions)
  }

  it should "wrap the underlying insertOne correctly" in {
    val insertDoc = Document("a" -> 1)
    (wrapped.insertOne _).expects(insertDoc).once()

    mongoCollection.insertOne(insertDoc)
  }

  it should "wrap the underlying insertMany correctly" in {
    val insertDocs = List(Document("a" -> 1))
    val insertOptions = new InsertManyOptions().ordered(false)

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/94
    // (wrapped.insertMany(_: util.List[Document])).expects(insertDocs.asJava).once()
    // (wrapped.insertMany(_: util.List[Document], _: InsertManyOptions)).expects(insertDocs.asJava, insertOptions).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.insertMany(insertDocs)
    mongoCollection.insertMany(insertDocs, insertOptions)
  }

  it should "wrap the underlying deleteOne correctly" in {
    (wrapped.deleteOne _).expects(filter).once()

    mongoCollection.deleteOne(filter)
  }

  it should "wrap the underlying deleteMany correctly" in {
    (wrapped.deleteMany _).expects(filter).once()

    mongoCollection.deleteMany(filter)
  }

  it should "wrap the underlying replaceOne correctly" in {
    val replacement = Document("a" -> 1)
    val updateOptions = new UpdateOptions().upsert(true)

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/93
    //(wrapped.replaceOne(_: Bson, _: Document)).expects(filter, replacement).once()
    //(wrapped.replaceOne(_, _: Document, _:UpdateOptions)).expects(filter, replacement, updateOptions).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.replaceOne(filter, replacement)
    mongoCollection.replaceOne(filter, replacement, updateOptions)
  }

  it should "wrap the underlying updateOne correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    (wrapped.updateOne(_: Bson, _: Bson)).expects(filter, update).once()
    (wrapped.updateOne(_: Bson, _: Bson, _: UpdateOptions)).expects(filter, update, updateOptions).once()

    mongoCollection.updateOne(filter, update)
    mongoCollection.updateOne(filter, update, updateOptions)
  }

  it should "wrap the underlying updateMany correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    (wrapped.updateMany(_: Bson, _: Bson)).expects(filter, update).once()
    (wrapped.updateMany(_: Bson, _: Bson, _: UpdateOptions)).expects(filter, update, updateOptions).once()

    mongoCollection.updateMany(filter, update)
    mongoCollection.updateMany(filter, update, updateOptions)
  }

  it should "wrap the underlying findOneAndDelete correctly" in {
    val options = new FindOneAndDeleteOptions().sort(Document("sort" -> 1))

    (wrapped.findOneAndDelete(_: Bson)).expects(filter).once()
    (wrapped.findOneAndDelete(_: Bson, _: FindOneAndDeleteOptions)).expects(filter, options).once()

    mongoCollection.findOneAndDelete(filter)
    mongoCollection.findOneAndDelete(filter, options)
  }

  it should "wrap the underlying findOneAndReplace correctly" in {
    val replacement = Document("a" -> 2)
    val options = new FindOneAndReplaceOptions().sort(Document("sort" -> 1))

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/93
    // (wrapped.findOneAndReplace(_: Bson, _: Document)).expects(filter, replacement).once()
    // (wrapped.findOneAndReplace(_: Bson, _: Document, _: FindOneAndReplaceOptions)).expects(filter, replacement, options).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.findOneAndReplace(filter, replacement)
    mongoCollection.findOneAndReplace(filter, replacement, options)
  }

  it should "wrap the underlying findOneAndUpdate correctly" in {
    val update = Document("a" -> 2)
    val options = new FindOneAndUpdateOptions().sort(Document("sort" -> 1))

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/93
    // (wrapped.findOneAndUpdate(_: Bson, _: Document)).expects(filter, update).once()
    // (wrapped.findOneAndUpdate(_: Bson, _: Document, _: FindOneAndUpdateOptions)).expects(filter, update, options).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.findOneAndUpdate(filter, update)
    mongoCollection.findOneAndUpdate(filter, update, options)
  }

  it should "wrap the underlying drop correctly" in {
    (wrapped.drop _).expects().once()

    mongoCollection.drop()
  }

  it should "wrap the underlying createIndex correctly" in {
    val index = Document("a" -> 1)
    val options = new IndexOptions().background(true)

    (wrapped.createIndex(_: Bson)).expects(index).once()
    (wrapped.createIndex(_: Bson, _: IndexOptions)).expects(index, options).once()

    mongoCollection.createIndex(index)
    mongoCollection.createIndex(index, options)
  }

  it should "wrap the underlying createIndexes correctly" in {
    val indexes = new IndexModel(Document("a" -> 1))

    // https://github.com/paulbutcher/ScalaMock/issues/93
    (wrapped.createIndexes(_: java.util.List[IndexModel])).expects(List(indexes).asJava).once()

    mongoCollection.createIndexes(List(indexes))
  }

  it should "wrap the underlying listIndexes correctly" in {
    (wrapped.listIndexes[Document] _).expects(classOf[Document]).once()
    (wrapped.listIndexes[BsonDocument] _).expects(classOf[BsonDocument]).once()

    mongoCollection.listIndexes()
    mongoCollection.listIndexes[BsonDocument]()
  }

  it should "wrap the underlying dropIndex correctly" in {
    (wrapped.dropIndex(_: String)).expects("indexName").once()

    mongoCollection.dropIndex("indexName")
  }

  it should "wrap the underlying dropIndexes correctly" in {
    (wrapped.dropIndexes _).expects().once()

    mongoCollection.dropIndexes()
  }

  it should "wrap the underlying renameCollection correctly" in {
    val newNamespace = new MongoNamespace("db", "coll")
    val options = new RenameCollectionOptions()

    (wrapped.renameCollection(_: MongoNamespace)).expects(newNamespace).once()
    (wrapped.renameCollection(_: MongoNamespace, _: RenameCollectionOptions)).expects(newNamespace, options).once()

    mongoCollection.renameCollection(newNamespace)
    mongoCollection.renameCollection(newNamespace, options)
  }

}
