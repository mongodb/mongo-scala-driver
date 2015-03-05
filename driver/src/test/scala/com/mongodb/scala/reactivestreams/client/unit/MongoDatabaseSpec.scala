package com.mongodb.scala.reactivestreams.client

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.reactivestreams.client.{ MongoDatabase => JMongoDatabase }
import com.mongodb.scala.reactivestreams.client.collection.Document
import com.mongodb.{ ReadPreference, WriteConcern }
import org.bson.BsonDocument
import org.bson.codecs.BsonValueCodecProvider
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.conversions.Bson
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MongoDatabaseSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoDatabase]
  val mongoDatabase = MongoDatabase(wrapped)
  val command = Document()
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
    val codecRegistry = fromProviders(new BsonValueCodecProvider())

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

  it should "call the underlying runCommand[T] when writing" in {
    (wrapped.runCommand[Document](_: Bson, _: Class[Document])).expects(command, classOf[Document]).once()
    (wrapped.runCommand[BsonDocument](_: Bson, _: Class[BsonDocument])).expects(command, classOf[BsonDocument]).once()

    mongoDatabase.runCommand(command)
    mongoDatabase.runCommand[BsonDocument](command)
  }

  it should "call the underlying runCommand[T] when reading" in {
    (wrapped.runCommand[Document](_: Bson, _: ReadPreference, _: Class[Document])).expects(command, readPreference, classOf[Document]).once()
    (wrapped.runCommand[BsonDocument](_: Bson, _: ReadPreference, _: Class[BsonDocument])).expects(command, readPreference, classOf[BsonDocument]).once()

    mongoDatabase.runCommand(command, readPreference)
    mongoDatabase.runCommand[BsonDocument](command, readPreference)
  }

  it should "call the underlying drop()" in {
    (wrapped.drop _).expects().once()

    mongoDatabase.drop()
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
