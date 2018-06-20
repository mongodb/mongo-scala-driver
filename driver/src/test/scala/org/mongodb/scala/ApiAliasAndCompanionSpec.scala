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

import java.lang.reflect.Modifier._

import scala.collection.JavaConverters._
import scala.reflect.runtime.currentMirror

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}
import org.scalatest.Inspectors.forEvery
import org.scalatest.{FlatSpec, Matchers}

class ApiAliasAndCompanionSpec extends FlatSpec with Matchers {

  val classFilter = (f: Class[_ <: Object]) => isPublic(f.getModifiers) && !f.getName.contains("$")

  "The scala package" should "mirror the com.mongodb package and com.mongodb.async.client" in {
    val packageName = "com.mongodb"
    val javaExclusions = Set("AsyncBatchCursor", "Block", "ConnectionString", "Function", "ServerCursor", "Majority", "MongoClients",
      "MongoIterable", "Observables", "SingleResultCallback", "GridFSBuckets", "DBRefCodec", "DBRefCodecProvider", "DBRef",
      "DocumentToDBRefTransformer", "ServerSession", "SessionContext", "DBObject", "BSONTimestampCodec", "UnixServerAddress", "Nullable",
      "BasicDBList", "NonNull", "DBObjectCodec", "DBObjectCodecProvider", "BasicDBObject", "BasicDBObjectBuilder",
      "NonNullApi")
    val scalaExclusions = Set("package", "internal", "result", "Helpers", "Document", "BulkWriteResult", "ScalaObservable",
      "ScalaWriteConcern", "ObservableImplicits", "Completed", "BoxedObservable", "BoxedObserver", "BoxedSubscription",
      "classTagToClassOf", "ReadConcernLevel", "bsonDocumentToDocument", "bsonDocumentToUntypedDocument", "documentToUntypedDocument",
      "BuildInfo", "SingleObservable", "ToSingleObservable", "ScalaSingleObservable")

    val classFilter = (f: Class[_ <: Object]) => {
      isPublic(f.getModifiers) &&
        !f.getName.contains("$") &&
        !f.getSimpleName.contains("Spec") &&
        !javaExclusions.contains(f.getSimpleName)
    }
    val filters = FilterBuilder.parse(
      """
        |-com.mongodb.annotations.*,
        |-com.mongodb.assertions.*,
        |-com.mongodb.binding.*,
        |-com.mongodb.bulk.*,
        |-com.mongodb.client.*,
        |-com.mongodb.connection.*,
        |-com.mongodb.diagnostics.*,
        |-com.mongodb.event.*,
        |-com.mongodb.internal.*,
        |-com.mongodb.management.*,
        |-com.mongodb.operation.*,
        |-com.mongodb.selector.*,
        |-com.mongodb.client.gridfs.*,
        |-com.mongodb.async.client.gridfs.*""".stripMargin
    )

    val exceptions = new Reflections(packageName).getSubTypesOf(classOf[MongoException]).asScala.map(_.getSimpleName).toSet +
      "MongoException" - "MongoGridFSException" - "MongoConfigurationException" - "MongoWriteConcernWithResponseException"

    val objects = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(packageName))
      .setScanners(new SubTypesScanner(false))
      .filterInputsBy(filters)).getSubTypesOf(classOf[Object])
      .asScala.filter(classFilter)
      .map(_.getSimpleName.replace("Iterable", "Observable")).toSet

    val wrapped = objects ++ exceptions
    val scalaPackageName = "org.mongodb.scala"
    val local = new Reflections(scalaPackageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(classFilter).filter(f => f.getPackage.getName == scalaPackageName)
      .map(_.getSimpleName).toSet ++ currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet -- scalaExclusions

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror parts of com.mongodb.connection in org.mongdb.scala.connection" in {
    val packageName = "com.mongodb.connection"
    val javaExclusions = Set("AsyncCompletionHandler", "AsyncConnection", "AsynchronousSocketChannelStreamFactory", "BufferProvider",
      "Builder", "BulkWriteBatchCombiner", "ChangeEvent", "ChangeListener", "Cluster", "ClusterDescription", "ClusterFactory",
      "ClusterId", "Connection", "ConnectionDescription", "ConnectionId", "DefaultClusterFactory", "DefaultRandomStringGenerator",
      "QueryResult", "RandomStringGenerator", "Server", "ServerDescription", "ServerId", "ServerVersion", "SocketStreamFactory", "Stream",
      "SplittablePayload")

    val filters = FilterBuilder.parse("-com.mongodb.connection.netty.*")
    val wrapped = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(packageName))
      .setScanners(new SubTypesScanner(false))
      .filterInputsBy(filters)).getSubTypesOf(classOf[Object])
      .asScala.filter(_.getPackage.getName == packageName)
      .filter(classFilter)
      .map(_.getSimpleName).toSet -- javaExclusions

    val scalaPackageName = "org.mongodb.scala.connection"
    val scalaExclusions = Set("package", "NettyStreamFactoryFactory", "NettyStreamFactoryFactoryBuilder",
      "AsynchronousSocketChannelStreamFactoryFactoryBuilder")
    val local = currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet -- scalaExclusions

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror all com.mongodb.client. into org.mongdb.scala." in {
    val packageName = "com.mongodb.client"
    val wrapped = new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(_.getPackage.getName == packageName)
      .filter(classFilter)
      .map(_.getSimpleName).toSet

    val scalaPackageName = "org.mongodb.scala"
    val local = new Reflections(scalaPackageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(_.getPackage.getName == scalaPackageName)
      .filter((f: Class[_ <: Object]) => isPublic(f.getModifiers))
      .map(_.getSimpleName.replace("$", "")).toSet

    forEvery(wrapped) { (className: String) => local should contain(className) }
  }

  it should "mirror all com.mongodb.client.model in org.mongdb.scala.model" in {
    val javaExclusions = Set("ParallelCollectionScanOptions")
    val packageName = "com.mongodb.client.model"

    val objectsAndEnums = new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object]).asScala ++
      new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Enum[_]]).asScala

    val wrapped = objectsAndEnums
      .filter(_.getPackage.getName == packageName)
      .filter(classFilter)
      .map(_.getSimpleName).toSet -- javaExclusions

    val scalaPackageName = "org.mongodb.scala.model"
    val localPackage = currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet
    val localObjects = new Reflections(scalaPackageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(classFilter).map(_.getSimpleName).toSet
    val scalaExclusions = Set("package", "FullDocument")
    val local = (localPackage ++ localObjects) -- scalaExclusions

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror all com.mongodb.client.model.geojson in org.mongdb.scala.model.geojson" in {
    val packageName = "com.mongodb.client.model.geojson"
    val wrapped = new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(_.getPackage.getName == packageName)
      .filter(classFilter)
      .map(_.getSimpleName).toSet ++ Set("GeoJsonObjectType", "CoordinateReferenceSystemType")

    val scalaPackageName = "org.mongodb.scala.model.geojson"
    val local = currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet - "package"

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror all com.mongodb.client.result in org.mongdb.scala.result" in {
    val packageName = "com.mongodb.client.result"
    val wrapped = new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(_.getPackage.getName == packageName)
      .filter(classFilter)
      .map(_.getSimpleName).toSet

    val scalaPackageName = "org.mongodb.scala.result"
    val local = currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet - "package"

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror all com.mongodb.WriteConcern in org.mongodb.scala.WriteConcern" in {
    val notMirrored = Set("SAFE", "serialVersionUID", "FSYNCED", "FSYNC_SAFE", "JOURNAL_SAFE", "REPLICAS_SAFE", "REPLICA_ACKNOWLEDGED",
      "NAMED_CONCERNS", "NORMAL", "majorityWriteConcern", "valueOf")
    val wrapped = (classOf[com.mongodb.WriteConcern].getDeclaredMethods ++ classOf[com.mongodb.WriteConcern].getDeclaredFields)
      .filter(f => isStatic(f.getModifiers) && !notMirrored.contains(f.getName)).map(_.getName).toSet

    val local = WriteConcern.getClass.getDeclaredMethods
      .filter(f => f.getName != "apply" && isPublic(f.getModifiers))
      .map(_.getName).toSet

    diff(local, wrapped) shouldBe empty
  }

  it should "mirror com.mongodb.async.client.gridfs in org.mongdb.scala.gridfs" in {
    val javaExclusions = Set("GridFSBuckets", "GridFSDownloadByNameOptions")
    val wrapped: Set[String] = Set("com.mongodb.async.client.gridfs", "com.mongodb.client.gridfs.model").flatMap(packageName =>
      new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
        .asScala.filter(_.getPackage.getName == packageName)
        .filter(classFilter)
        .map(_.getSimpleName.replace("Iterable", "Observable")).toSet) -- javaExclusions + "MongoGridFSException"

    val scalaPackageName = "org.mongodb.scala.gridfs"
    val scalaExclusions = Set("package", "ScalaAsyncInputStreamToJava", "ScalaAsyncOutputStreamToJava", "JavaAsyncOutputStreamToScala",
      "JavaAsyncInputStreamToScala")
    val local = new Reflections(scalaPackageName, new SubTypesScanner(false)).getSubTypesOf(classOf[Object])
      .asScala.filter(classFilter).filter(f => f.getPackage.getName == scalaPackageName)
      .map(_.getSimpleName).toSet ++ currentMirror.staticPackage(scalaPackageName).info.decls.map(_.name.toString).toSet -- scalaExclusions

    diff(local, wrapped) shouldBe empty
  }

  def diff(a: Set[String], b: Set[String]): Set[String] = a.diff(b) ++ b.diff(a)
}
