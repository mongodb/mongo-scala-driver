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
import com.mongodb.connection.ClusterSettings
import com.mongodb.async.client.{MongoClient => JMongoClient}

import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class MongoClientSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoClient]
  val mongoClient = new MongoClient(wrapped)

  "MongoClient" should "have the same methods as the wrapped MongoClient" in {
    val wrapped = classOf[JMongoClient].getMethods.map(_.getName).toSet
    val local = classOf[MongoClient].getMethods.map(_.getName).toSet

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
    val mongoClient = MongoClient(MongoClientSettings.builder().clusterSettings(clusterSettings).build())

    mongoClient.settings.getClusterSettings.getHosts.get(0) shouldBe serverAddress
  }

  it should "call the underlying getSettings" in {
    wrapped.expects('getSettings)().once()

    mongoClient.settings
  }

  it should "call the underlying getDatabase" in {
    wrapped.expects('getDatabase)("dbName").once()

    mongoClient.getDatabase("dbName")
  }

  it should "call the underlying close" in {
    wrapped.expects('close)().once()

    mongoClient.close()
  }

  it should "call the underlying listDatabases[T]" in {
    wrapped.expects('listDatabases)(classOf[Document]).once()
    wrapped.expects('listDatabases)(classOf[BsonDocument]).once()

    mongoClient.listDatabases()
    mongoClient.listDatabases[BsonDocument]()
  }

  it should "call the underlying listDatabaseNames" in {
    wrapped.expects('listDatabaseNames)().once()

    mongoClient.listDatabaseNames()
  }

}
