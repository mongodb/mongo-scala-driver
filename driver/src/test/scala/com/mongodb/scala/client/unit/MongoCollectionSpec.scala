package com.mongodb.scala.client

import scala.collection.JavaConverters._

import org.bson.BsonDocument
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import com.mongodb.scala.Implicits._
import com.mongodb.{ MongoNamespace, ReadPreference, WriteConcern }
import com.mongodb.client.model._
import com.mongodb.client.result.{ DeleteResult, UpdateResult }
import com.mongodb.async.client.{ MongoCollection => JMongoCollection }

import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MongoCollectionSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoCollection[Document]]
  val mongoCollection = MongoCollection[Document](wrapped)
  val readPreference = ReadPreference.secondary()
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

  it should "return the underlying withDocumentClass" in {
    wrapped.expects('withDocumentClass)(classOf[Document]).once()
    wrapped.expects('withDocumentClass)(classOf[BsonDocument]).once()

    mongoCollection.withDocumentClass[Document]()
    mongoCollection.withDocumentClass[BsonDocument]()
  }

  it should "return the underlying count" in {
    val countOptions = new CountOptions().hintString("Hint")

    wrapped.expects('count)(*).once()
    wrapped.expects('count)(filter, *).once()
    wrapped.expects('count)(filter, countOptions, *).once()

    mongoCollection.count().subscribe(observer[Long])
    mongoCollection.count(filter).subscribe(observer[Long])
    mongoCollection.count(filter, countOptions).subscribe(observer[Long])
  }

  it should "wrap the underlying DistinctObservable correctly" in {
    wrapped.expects('distinct)("fieldName", classOf[String]).once()
    wrapped.expects('distinct)("fieldName", filter, classOf[String]).once()

    mongoCollection.distinct[String]("fieldName")
    mongoCollection.distinct[String]("fieldName", filter)
  }

  it should "wrap the underlying FindObservable correctly" in {
    wrapped.expects('find)(classOf[Document]).once()
    wrapped.expects('find)(classOf[BsonDocument]).once()

    mongoCollection.find() shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument]() shouldBe a[FindObservable[_]]

    wrapped.expects('find)(filter, classOf[Document]).once()
    wrapped.expects('find)(filter, classOf[BsonDocument]).once()

    mongoCollection.find(filter) shouldBe a[FindObservable[_]]
    mongoCollection.find[BsonDocument](filter) shouldBe a[FindObservable[_]]
  }

  it should "wrap the underlying AggregateObservable correctly" in {
    val pipeline = List(Document("$match" -> 1))

    // Todo - currently a bug in scala mock prevents this.
    // https://github.com/paulbutcher/ScalaMock/issues/94
    // wrapped.expects('aggregate)(pipeline.asJava, classOf[Document]).once()
    // wrapped.expects('aggregate)(pipeline.asJava, classOf[BsonDocument]).once()
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.aggregate(pipeline) shouldBe a[AggregateObservable[_]]
    mongoCollection.aggregate[BsonDocument](pipeline) shouldBe a[AggregateObservable[_]]
  }

  it should "wrap the underlying MapReduceObservable correctly" in {
    wrapped.expects('mapReduce)("map", "reduce", classOf[Document]).once()
    wrapped.expects('mapReduce)("map", "reduce", classOf[BsonDocument]).once()

    mongoCollection.mapReduce("map", "reduce") shouldBe a[MapReduceObservable[_]]
    mongoCollection.mapReduce[BsonDocument]("map", "reduce") shouldBe a[MapReduceObservable[_]]
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
    // wrapped.expects('bulkWrite)(bulkWrite.asJava).once()
    // (wrapped.bulkWrite(_: util.List[_ <: WriteModel[_ <: Document]], _: BulkWriteOptions)).expects(bulkWrite.asJava, bulkWriteOptions)
    val mongoCollection = MongoCollection(stub[JMongoCollection[Document]])

    mongoCollection.bulkWrite(bulkRequests) // Bug with matcher prevents matching against [Observable[_]]
    mongoCollection.bulkWrite(bulkRequests, bulkWriteOptions)
  }

  it should "wrap the underlying insertOne correctly" in {
    val insertDoc = Document("a" -> 1)
    wrapped.expects('insertOne)(insertDoc, *).once()

    mongoCollection.insertOne(insertDoc).subscribe(observer[Void])
  }

  it should "wrap the underlying insertMany correctly" in {
    val insertDocs = List(Document("a" -> 1))
    val insertOptions = new InsertManyOptions().ordered(false)

    wrapped.expects('insertMany)(insertDocs.asJava, *).once()
    wrapped.expects('insertMany)(insertDocs.asJava, insertOptions, *).once()

    mongoCollection.insertMany(insertDocs).subscribe(observer[Void])
    mongoCollection.insertMany(insertDocs, insertOptions).subscribe(observer[Void])
  }

  it should "wrap the underlying deleteOne correctly" in {
    wrapped.expects('deleteOne)(filter, *).once()

    mongoCollection.deleteOne(filter).subscribe(observer[DeleteResult])
  }

  it should "wrap the underlying deleteMany correctly" in {
    wrapped.expects('deleteMany)(filter, *).once()

    mongoCollection.deleteMany(filter).subscribe(observer[DeleteResult])
  }

  it should "wrap the underlying replaceOne correctly" in {
    val replacement = Document("a" -> 1)
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('replaceOne)(filter, replacement, *).once()
    wrapped.expects('replaceOne)(filter, replacement, updateOptions, *).once()

    mongoCollection.replaceOne(filter, replacement).subscribe(observer[UpdateResult])
    mongoCollection.replaceOne(filter, replacement, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying updateOne correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('updateOne)(filter, update, *).once()
    wrapped.expects('updateOne)(filter, update, updateOptions, *).once()

    mongoCollection.updateOne(filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateOne(filter, update, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying updateMany correctly" in {
    val update = Document("$set" -> Document("a" -> 2))
    val updateOptions = new UpdateOptions().upsert(true)

    wrapped.expects('updateMany)(filter, update, *).once()
    wrapped.expects('updateMany)(filter, update, updateOptions, *).once()

    mongoCollection.updateMany(filter, update).subscribe(observer[UpdateResult])
    mongoCollection.updateMany(filter, update, updateOptions).subscribe(observer[UpdateResult])
  }

  it should "wrap the underlying findOneAndDelete correctly" in {
    val options = new FindOneAndDeleteOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndDelete)(filter, *).once()
    wrapped.expects('findOneAndDelete)(filter, options, *).once()

    mongoCollection.findOneAndDelete(filter).subscribe(observer[Document])
    mongoCollection.findOneAndDelete(filter, options).subscribe(observer[Document])
  }

  it should "wrap the underlying findOneAndReplace correctly" in {
    val replacement = Document("a" -> 2)
    val options = new FindOneAndReplaceOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndReplace)(filter, replacement, *).once()
    wrapped.expects('findOneAndReplace)(filter, replacement, options, *).once()

    mongoCollection.findOneAndReplace(filter, replacement).subscribe(observer[Document])
    mongoCollection.findOneAndReplace(filter, replacement, options).subscribe(observer[Document])
  }

  it should "wrap the underlying findOneAndUpdate correctly" in {
    val update = Document("a" -> 2)
    val options = new FindOneAndUpdateOptions().sort(Document("sort" -> 1))

    wrapped.expects('findOneAndUpdate)(filter, update, *).once()
    wrapped.expects('findOneAndUpdate)(filter, update, options, *).once()

    mongoCollection.findOneAndUpdate(filter, update).subscribe(observer[Document])
    mongoCollection.findOneAndUpdate(filter, update, options).subscribe(observer[Document])
  }

  it should "wrap the underlying drop correctly" in {
    wrapped.expects('drop)(*).once()

    mongoCollection.drop().subscribe(observer[Void])
  }

  it should "wrap the underlying createIndex correctly" in {
    val index = Document("a" -> 1)
    val options = new IndexOptions().background(true)

    wrapped.expects('createIndex)(index, *).once()
    wrapped.expects('createIndex)(index, options, *).once()

    mongoCollection.createIndex(index).subscribe(observer[String])
    mongoCollection.createIndex(index, options).subscribe(observer[String])
  }

  it should "wrap the underlying createIndexes correctly" in {
    val indexes = new IndexModel(Document("a" -> 1))

    // https://github.com/paulbutcher/ScalaMock/issues/93
    wrapped.expects('createIndexes)(List(indexes).asJava, *).once()

    mongoCollection.createIndexes(List(indexes)).subscribe(observer[String])
  }

  it should "wrap the underlying listIndexes correctly" in {
    wrapped.expects('listIndexes)(classOf[Document]).once()
    wrapped.expects('listIndexes)(classOf[BsonDocument]).once()

    mongoCollection.listIndexes()
    mongoCollection.listIndexes[BsonDocument]()
  }

  it should "wrap the underlying dropIndex correctly" in {
    wrapped.expects('dropIndex)("indexName", *).once()

    mongoCollection.dropIndex("indexName").subscribe(observer[Void])
  }

  it should "wrap the underlying dropIndexes correctly" in {
    wrapped.expects('dropIndexes)(*).once()

    mongoCollection.dropIndexes().subscribe(observer[Void])
  }

  it should "wrap the underlying renameCollection correctly" in {
    val newNamespace = new MongoNamespace("db", "coll")
    val options = new RenameCollectionOptions()

    wrapped.expects('renameCollection)(newNamespace, *).once()
    wrapped.expects('renameCollection)(newNamespace, options, *).once()

    mongoCollection.renameCollection(newNamespace).subscribe(observer[Void])
    mongoCollection.renameCollection(newNamespace, options).subscribe(observer[Void])
  }

}
