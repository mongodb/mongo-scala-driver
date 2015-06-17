package org.mongodb.scala.unit
import org.bson.BsonDocument
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.async.client.{ ListCollectionsIterable, MongoDatabase => JMongoDatabase }

import org.mongodb.scala._
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MongoDatabaseSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoDatabase]
  val mongoDatabase = MongoDatabase(wrapped)
  val command = Document()
  val readPreference = ReadPreference.secondary()
  def observer[T] = new Observer[T]() {
    override def onError(throwable: Throwable): Unit = {}
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
    override def onComplete(): Unit = {}
    override def onNext(doc: T): Unit = {}
  }

  "MongoDatabase" should "have the same methods as the wrapped MongoDatabase" in {
    val wrapped = classOf[JMongoDatabase].getMethods.map(_.getName).toSet
    val local = classOf[MongoDatabase].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "return the underlying getCollection[T]" in {
    wrapped.expects('getCollection)("collectionName", classOf[Document]).once()
    wrapped.expects('getCollection)("collectionName", classOf[BsonDocument]).once()

    mongoDatabase.getCollection("collectionName")
    mongoDatabase.getCollection[BsonDocument]("collectionName")
  }

  it should "return the underlying getName" in {
    wrapped.expects('getName)().once()

    mongoDatabase.name
  }

  it should "return the underlying getCodecRegistry" in {
    wrapped.expects('getCodecRegistry)().once()

    mongoDatabase.codecRegistry
  }

  it should "return the underlying getReadPreference" in {
    wrapped.expects('getReadPreference)().once()

    mongoDatabase.readPreference
  }

  it should "return the underlying getWriteConcern" in {
    wrapped.expects('getWriteConcern)().once()

    mongoDatabase.writeConcern
  }

  it should "return the underlying withCodecRegistry" in {
    val codecRegistry = fromProviders(new BsonValueCodecProvider())

    wrapped.expects('withCodecRegistry)(codecRegistry).once()

    mongoDatabase.withCodecRegistry(codecRegistry)
  }

  it should "return the underlying withReadPreference" in {
    wrapped.expects('withReadPreference)(readPreference).once()

    mongoDatabase.withReadPreference(readPreference)
  }

  it should "return the underlying withWriteConcern" in {
    val writeConcern = WriteConcern.MAJORITY
    wrapped.expects('withWriteConcern)(writeConcern).once()

    mongoDatabase.withWriteConcern(writeConcern)
  }

  it should "call the underlying runCommand[T] when writing" in {
    wrapped.expects('runCommand)(command, classOf[Document], *).once()
    wrapped.expects('runCommand)(command, classOf[BsonDocument], *).once()

    mongoDatabase.runCommand(command).subscribe(observer[Document])
    mongoDatabase.runCommand[BsonDocument](command).subscribe(observer[BsonDocument])
  }

  it should "call the underlying runCommand[T] when reading" in {
    wrapped.expects('runCommand)(command, readPreference, classOf[Document], *).once()
    wrapped.expects('runCommand)(command, readPreference, classOf[BsonDocument], *).once()

    mongoDatabase.runCommand(command, readPreference).subscribe(observer[Document])
    mongoDatabase.runCommand[BsonDocument](command, readPreference).subscribe(observer[BsonDocument])
  }

  it should "call the underlying drop()" in {
    wrapped.expects('drop)(*).once()

    mongoDatabase.drop().subscribe(observer[Void])
  }

  it should "call the underlying listCollectionNames()" in {
    wrapped.expects('listCollectionNames)().once()

    mongoDatabase.listCollectionNames()
  }

  it should "call the underlying listCollections()" in {
    wrapped.expects('listCollections)(*).returns(stub[ListCollectionsIterable[Document]]).once()
    wrapped.expects('listCollections)(classOf[BsonDocument]).returns(stub[ListCollectionsIterable[BsonDocument]]).once()

    mongoDatabase.listCollections().subscribe(observer[Document])
    mongoDatabase.listCollections[BsonDocument]().subscribe(observer[BsonDocument])
  }

  it should "call the underlying createCollection()" in {
    val options = new CreateCollectionOptions().capped(true)
    wrapped.expects('createCollection)("collectionName", *).once()
    wrapped.expects('createCollection)("collectionName", options, *).once()

    mongoDatabase.createCollection("collectionName").subscribe(observer[Void])
    mongoDatabase.createCollection("collectionName", options).subscribe(observer[Void])
  }

}
