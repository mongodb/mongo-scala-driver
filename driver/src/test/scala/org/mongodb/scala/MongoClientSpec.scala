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

import com.mongodb.Block

import scala.collection.JavaConverters._
import org.bson.BsonDocument
import com.mongodb.async.client.{MongoClient => JMongoClient}
import com.mongodb.connection.ClusterSettings
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class MongoClientSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoClient]
  val clientSession = mock[ClientSession]
  val mongoClient = new MongoClient(wrapped)
  def observer[T] = new Observer[T]() {
    override def onError(throwable: Throwable): Unit = {}
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
    override def onComplete(): Unit = {}
    override def onNext(doc: T): Unit = {}
  }

  "MongoClient" should "have the same methods as the wrapped MongoClient" in {
    val wrapped = classOf[JMongoClient].getMethods.map(_.getName)
    val local = classOf[MongoClient].getMethods.map(_.getName)

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "default to localhost:27107" in {
    val serverAddress = new ServerAddress("localhost", 27017)
    val mongoClient = MongoClient()

    mongoClient.settings.getClusterSettings.getHosts.asScala.head shouldBe serverAddress
  }

  it should "apply read preference from connection string to settings" in {
    MongoClient("mongodb://localhost/").settings.getReadPreference should equal(ReadPreference.primary())
    MongoClient("mongodb://localhost/?readPreference=secondaryPreferred").settings.getReadPreference should equal(ReadPreference.secondaryPreferred())
  }

  it should "apply read concern from connection string to settings" in {
    MongoClient("mongodb://localhost/").settings.getReadConcern should equal(ReadConcern.DEFAULT)
    MongoClient("mongodb://localhost/?readConcernLevel=local").settings.getReadConcern should equal(ReadConcern.LOCAL)
  }

  it should "apply write concern from connection string to settings" in {
    MongoClient("mongodb://localhost/").settings.getWriteConcern should equal(WriteConcern.ACKNOWLEDGED)
    MongoClient("mongodb://localhost/?w=majority").settings.getWriteConcern should equal(WriteConcern.MAJORITY)
  }

  it should "accept MongoClientSettings" in {
    val serverAddress = new ServerAddress("localhost", 27020)
    val clusterSettings = ClusterSettings.builder().hosts(List(serverAddress).asJava).build()
    val mongoClient = MongoClient(MongoClientSettings.builder()
      .applyToClusterSettings(new Block[ClusterSettings.Builder] {
        override def apply(b: ClusterSettings.Builder): Unit = b.applySettings(clusterSettings)
      }).build())

    mongoClient.settings.getClusterSettings.getHosts.get(0) shouldBe serverAddress
  }

  it should "accept MongoDriverInformation" in {
    val driverInformation = MongoDriverInformation.builder().driverName("test").driverVersion("1.2.0").build()
    MongoClient("mongodb://localhost", Some(driverInformation))
  }

  it should "call the underlying getSettings" in {
    wrapped.expects(Symbol("getSettings"))().once()

    mongoClient.settings
  }

  it should "call the underlying getDatabase" in {
    wrapped.expects(Symbol("getDatabase"))("dbName").once()

    mongoClient.getDatabase("dbName")
  }

  it should "call the underlying close" in {
    wrapped.expects(Symbol("close"))().once()

    mongoClient.close()
  }

  it should "call the underlying startSession" in {
    val clientSessionOptions = ClientSessionOptions.builder.build()
    wrapped.expects(Symbol("startSession"))(clientSessionOptions, *).once()

    mongoClient.startSession(clientSessionOptions).subscribe(observer[ClientSession])
  }

  it should "call the underlying listDatabases[T]" in {
    wrapped.expects(Symbol("listDatabases"))(classOf[Document]).once()
    wrapped.expects(Symbol("listDatabases"))(clientSession, classOf[Document]).once()
    wrapped.expects(Symbol("listDatabases"))(classOf[BsonDocument]).once()
    wrapped.expects(Symbol("listDatabases"))(clientSession, classOf[BsonDocument]).once()

    mongoClient.listDatabases()
    mongoClient.listDatabases(clientSession)
    mongoClient.listDatabases[BsonDocument]()
    mongoClient.listDatabases[BsonDocument](clientSession)
  }

  it should "call the underlying listDatabaseNames" in {
    wrapped.expects(Symbol("listDatabaseNames"))().once()
    wrapped.expects(Symbol("listDatabaseNames"))(clientSession).once()

    mongoClient.listDatabaseNames()
    mongoClient.listDatabaseNames(clientSession)
  }

  it should "call the underlying watch" in {
    val pipeline = List(Document("$match" -> 1))

    wrapped.expects(Symbol("watch"))(classOf[Document]).once()
    wrapped.expects(Symbol("watch"))(pipeline.asJava, classOf[Document]).once()
    wrapped.expects(Symbol("watch"))(pipeline.asJava, classOf[BsonDocument]).once()
    wrapped.expects(Symbol("watch"))(clientSession, pipeline.asJava, classOf[Document]).once()
    wrapped.expects(Symbol("watch"))(clientSession, pipeline.asJava, classOf[BsonDocument]).once()

    mongoClient.watch() shouldBe a[ChangeStreamObservable[_]]
    mongoClient.watch(pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoClient.watch[BsonDocument](pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoClient.watch(clientSession, pipeline) shouldBe a[ChangeStreamObservable[_]]
    mongoClient.watch[BsonDocument](clientSession, pipeline) shouldBe a[ChangeStreamObservable[_]]
  }

}
