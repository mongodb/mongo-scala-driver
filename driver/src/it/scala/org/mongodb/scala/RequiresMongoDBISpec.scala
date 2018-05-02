/**
 * Copyright 2014-2015 MongoDB, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * For questions and comments about this product, please see the project page at:
 *
 * https://github.com/mongodb/mongo-scala-driver
 */

package org.mongodb.scala

import com.mongodb.ConnectionString

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Properties, Success, Try}
import com.mongodb.connection.ServerVersion
import org.mongodb.scala.bson.BsonString
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

trait RequiresMongoDBISpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(60, Seconds), interval = Span(5, Millis))
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val DEFAULT_URI: String = "mongodb://localhost:27017/"
  private val MONGODB_URI_SYSTEM_PROPERTY_NAME: String = "org.mongodb.test.uri"
  private val WAIT_DURATION: Duration = 10.seconds
  private val DB_PREFIX = "mongo-scala-"
  private var _currentTestName: Option[String] = None
  private var mongoDBOnline: Boolean = false

  protected override def runTest(testName: String, args: Args): Status = {
    _currentTestName = Some(testName.split("should")(1))
    mongoDBOnline = isMongoDBOnline()
    super.runTest(testName, args)
  }

  /**
   * The database name to use for this test
   */
  def databaseName: String = DB_PREFIX + suiteName

  /**
   * The collection name to use for this test
   */
  def collectionName: String = _currentTestName.getOrElse(suiteName).filter(_.isLetterOrDigit)

  val mongoClientURI: String = Properties.propOrElse(MONGODB_URI_SYSTEM_PROPERTY_NAME, DEFAULT_URI)

  def mongoClient(): MongoClient = MongoClient(mongoClientURI)

  def isMongoDBOnline(): Boolean = {
    Try(Await.result(MongoClient(mongoClientURI).listDatabaseNames(), WAIT_DURATION)).isSuccess
  }

  def hasSingleHost(): Boolean = {
    new ConnectionString(mongoClientURI).getHosts.size() == 1
  }

  def checkMongoDB() {
    if (!mongoDBOnline) {
      cancel("No Available Database")
    }
  }

  def withDatabase(dbName: String)(testCode: MongoDatabase => Any) {
    checkMongoDB()
    val client = mongoClient()
    val databaseName = if (dbName.startsWith(DB_PREFIX)) dbName.take(63) else s"$DB_PREFIX$dbName".take(63)
    val mongoDatabase = client.getDatabase(databaseName)
    try testCode(mongoDatabase) // "loan" the fixture to the test
    finally {
      // clean up the fixture
      Await.result(mongoDatabase.drop(), WAIT_DURATION)
      client.close()
    }
  }

  def withDatabase(testCode: MongoDatabase => Any): Unit = withDatabase(databaseName)(testCode: MongoDatabase => Any)

  def withCollection(testCode: MongoCollection[Document] => Any) {
    checkMongoDB()
    val client = mongoClient()
    val mongoDatabase = client.getDatabase(databaseName)
    val mongoCollection = mongoDatabase.getCollection(collectionName)
    try testCode(mongoCollection) // "loan" the fixture to the test
    finally {
      // clean up the fixture
      Await.result(mongoCollection.drop(), WAIT_DURATION)
      client.close()
    }
  }

  lazy val buildInfo: Document = {
    if (mongoDBOnline) {
      mongoClient().getDatabase("admin").runCommand(Document("buildInfo" -> 1)).futureValue
    } else {
      Document()
    }
  }

  def serverVersionAtLeast(minServerVersion: List[Int]): Boolean = {
    buildInfo.get[BsonString]("version") match {
      case Some(version) =>
        val serverVersion = version.getValue.split("\\D+").map(_.toInt).padTo(3, 0).take(3).toList.asJava
        new ServerVersion(serverVersion.asInstanceOf[java.util.List[Integer]]).compareTo(
          new ServerVersion(minServerVersion.asJava.asInstanceOf[java.util.List[Integer]])) >= 0
      case None => false
    }
  }

  def serverVersionLessThan(maxServerVersion: List[Int]): Boolean = {
    buildInfo.get[BsonString]("version") match {
      case Some(version) =>
        val serverVersion = version.getValue.split("\\D+").map(_.toInt).padTo(3, 0).take(3).toList.asJava
        new ServerVersion(serverVersion.asInstanceOf[java.util.List[Integer]]).compareTo(
          new ServerVersion(maxServerVersion.asJava.asInstanceOf[java.util.List[Integer]])) < 0
      case None => false
    }
  }

  override def beforeAll() {
    if (mongoDBOnline) {
      val client = mongoClient()
      Await.result(client.getDatabase(databaseName).drop(), WAIT_DURATION)
      client.close()
    }
  }

  override def afterAll() {
    if (mongoDBOnline) {
      val client = mongoClient()
      Await.result(client.getDatabase(databaseName).drop(), WAIT_DURATION)
      client.close()
    }
  }

  Runtime.getRuntime.addShutdownHook(new ShutdownHook())

  private[mongodb] class ShutdownHook extends Thread {
    override def run() {
      mongoClient().getDatabase(databaseName).drop()
    }
  }

  implicit def observableToFuture[TResult](observable: Observable[TResult]): Future[Seq[TResult]] = observable.toFuture()

  implicit def observableToFutureConcept[T](observable: Observable[T]): FutureConcept[Seq[T]] = {
    val future: Future[Seq[T]] = observable
    new FutureConcept[Seq[T]] {
      def eitherValue: Option[Either[Throwable, Seq[T]]] = {
        future.value.map {
          case Success(o) => Right(o)
          case Failure(e) => Left(e)
        }
      }
      def isExpired: Boolean = false

      // Scala Futures themselves don't support the notion of a timeout
      def isCanceled: Boolean = false // Scala Futures don't seem to be cancelable either
    }
  }

  implicit def observableToFuture[TResult](observable: SingleObservable[TResult]): Future[TResult] = observable.toFuture()
  implicit def observableToFutureConcept[T](observable: SingleObservable[T]): FutureConcept[T] = {
    val future: Future[T] = observable.toFuture()
    new FutureConcept[T] {
      def eitherValue: Option[Either[Throwable, T]] = {
        future.value.map {
          case Success(o) => Right(o)
          case Failure(e) => Left(e)
        }
      }
      def isExpired: Boolean = false

      // Scala Futures themselves don't support the notion of a timeout
      def isCanceled: Boolean = false // Scala Futures don't seem to be cancelable either
    }
  }

}
