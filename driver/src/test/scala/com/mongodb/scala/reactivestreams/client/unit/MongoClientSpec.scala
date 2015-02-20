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

package com.mongodb.scala.reactivestreams.client

import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientOptions
import com.mongodb.connection.ClusterSettings
import com.mongodb.reactivestreams.client.{ MongoClient => JMongoClient }
import com.mongodb.scala.reactivestreams.client.collection.Document
import org.bson.BsonDocument
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

class MongoClientSpec extends FlatSpec with Matchers with MockFactory {

  val wrapped = mock[JMongoClient]
  val mongoClient = MongoClient(wrapped)

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

    mongoClient.options.getClusterSettings.getHosts().asScala.head shouldBe serverAddress
  }

  it should "accept MongoClientOptions" in {
    val serverAddress = new ServerAddress("localhost", 27020)
    val clusterSettings = ClusterSettings.builder().hosts(List(serverAddress).asJava).build()
    val mongoClient = MongoClient(MongoClientOptions.builder().clusterSettings(clusterSettings).build())

    mongoClient.options.getClusterSettings.getHosts().get(0) shouldBe serverAddress
  }

  it should "call the underlying getOptions" in {
    (wrapped.getOptions _).expects().once()

    mongoClient.options
  }

  it should "call the underlying getDatabase" in {
    (wrapped.getDatabase _).expects("dbName").once()

    mongoClient.getDatabase("dbName")
  }

  it should "call the underlying close" in {
    (wrapped.close _).expects().once()

    mongoClient.close()
  }

  it should "call the underlying listDatabases[T]" in {
    (wrapped.listDatabases[Document] _).expects(classOf[Document]).once()
    (wrapped.listDatabases[BsonDocument] _).expects(classOf[BsonDocument]).once()

    mongoClient.listDatabases()
    mongoClient.listDatabases[BsonDocument]()
  }

  it should "call the underlying listDatabaseNames" in {
    (wrapped.listDatabaseNames _).expects().once()

    mongoClient.listDatabaseNames()
  }

}
