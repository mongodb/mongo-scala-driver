package com.mongodb.scala.reactivestreams.client

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.reactivestreams.client.{ MongoDatabase => JMongoDatabase }
import com.mongodb.{ ReadPreference, WriteConcern }
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.RootCodecRegistry
import org.bson.{ BsonDocument, Document }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

class MongoDatabaseSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoDatabase]
  val mongoDatabase = MongoDatabase(wrapped)
  val command = new Document()
  val readPreference = ReadPreference.secondary()

  "MongoDatabase" should "have the same methods as the wrapped MongoDatabase" in {
    val wrapped = classOf[JMongoDatabase].getMethods.map(_.getName).toSet
    val local = classOf[MongoDatabase].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "return the underlying getCollection[T]" in {
    (wrapped.getCollection[Document] _).expects("collectionName", classOf[Document]).once()
    (wrapped.getCollection[BsonDocument] _).expects("collectionName", classOf[BsonDocument]).once()

    mongoDatabase.getCollection("collectionName")
    mongoDatabase.getCollection[BsonDocument]("collectionName")
  }

  it should "return the underlying getName" in {
    (wrapped.getName _).expects().once()

    mongoDatabase.name
  }

  it should "return the underlying getCodecRegistry" in {
    (wrapped.getCodecRegistry _).expects().once()

    mongoDatabase.codecRegistry
  }

  it should "return the underlying getReadPreference" in {
    (wrapped.getReadPreference _).expects().once()

    mongoDatabase.readPreference
  }

  it should "return the underlying getWriteConcern" in {
    (wrapped.getWriteConcern _).expects().once()

    mongoDatabase.writeConcern
  }

  it should "return the underlying withCodecRegistry" in {
    val codecRegistry = new RootCodecRegistry(List(new BsonValueCodecProvider()).asJava)

    (wrapped.withCodecRegistry _).expects(codecRegistry).once()

    mongoDatabase.withCodecRegistry(codecRegistry)
  }

  it should "return the underlying withReadPreference" in {
    (wrapped.withReadPreference _).expects(readPreference).once()

    mongoDatabase.withReadPreference(readPreference)
  }

  it should "return the underlying withWriteConcern" in {
    val writeConcern = WriteConcern.MAJORITY
    (wrapped.withWriteConcern _).expects(writeConcern).once()

    mongoDatabase.withWriteConcern(writeConcern)
  }

  it should "call the underlying executeCommand[T] when writing" in {
    (wrapped.executeCommand[Document](_: Any, _: Class[Document])).expects(command, classOf[Document]).once()
    (wrapped.executeCommand[BsonDocument](_: Any, _: Class[BsonDocument])).expects(command, classOf[BsonDocument]).once()

    mongoDatabase.executeCommand(command)
    mongoDatabase.executeCommand[BsonDocument](command)
  }

  it should "call the underlying executeCommand[T] when reading" in {
    (wrapped.executeCommand[Document](_: Any, _: ReadPreference, _: Class[Document])).expects(command, readPreference, classOf[Document]).once()
    (wrapped.executeCommand[BsonDocument](_: Any, _: ReadPreference, _: Class[BsonDocument])).expects(command, readPreference, classOf[BsonDocument]).once()

    mongoDatabase.executeCommand(command, readPreference)
    mongoDatabase.executeCommand[BsonDocument](command, readPreference)
  }

  it should "call the underlying dropDatabase()" in {
    (wrapped.dropDatabase _).expects().once()

    mongoDatabase.dropDatabase()
  }

  it should "call the underlying listCollectionNames()" in {
    (wrapped.listCollectionNames _).expects().once()

    mongoDatabase.listCollectionNames()
  }

  it should "call the underlying listCollections()" in {
    (wrapped.listCollections[Document] _).expects(classOf[Document]).once()
    (wrapped.listCollections[BsonDocument] _).expects(classOf[BsonDocument]).once()

    mongoDatabase.listCollections()
    mongoDatabase.listCollections[BsonDocument]()
  }

  it should "call the underlying createCollection()" in {
    val options = new CreateCollectionOptions().capped(true)
    (wrapped.createCollection(_: String)).expects("collectionName").once()
    (wrapped.createCollection(_: String, _: CreateCollectionOptions)).expects("collectionName", options).once()

    mongoDatabase.createCollection("collectionName")
    mongoDatabase.createCollection("collectionName", options)
  }

}
