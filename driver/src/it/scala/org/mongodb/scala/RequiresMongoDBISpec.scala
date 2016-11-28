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

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Properties, Success, Try}

import com.mongodb.connection.ServerVersion

import org.mongodb.scala.bson.BsonString
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

trait RequiresMongoDBISpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val defaultPatience = PatienceConfig(timeout = Span(60, Seconds), interval = Span(5, Millis))
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private val DEFAULT_URI: String = "mongodb://localhost:27017/"
  private val MONGODB_URI_SYSTEM_PROPERTY_NAME: String = "org.mongodb.test.uri"
  private val WAIT_DURATION = Duration(10, "second")
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

  val mongoClientURI = Properties.propOrElse(MONGODB_URI_SYSTEM_PROPERTY_NAME, DEFAULT_URI)

  def mongoClient() = MongoClient(mongoClientURI)

  def isMongoDBOnline(): Boolean = {
    Try(Await.result(MongoClient(mongoClientURI).listDatabaseNames(), Duration(10, "second"))).isSuccess
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

  def withDatabase(testCode: MongoDatabase => Any): Unit = withDatabase(collectionName)(testCode: MongoDatabase => Any)

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
      mongoClient().getDatabase("admin").runCommand(Document("buildInfo" -> 1)).futureValue.head
    } else {
      Document()
    }
  }

  def serverVersionAtLeast(minServerVersion: List[Int]): Boolean = {
    buildInfo.get[BsonString]("version") match {
      case Some(version) =>
        val serverVersion = version.getValue.split("\\.").map(_.toInt).padTo(3, 0).take(3).toList.asJava
        new ServerVersion(serverVersion.asInstanceOf[java.util.List[Integer]]).compareTo(
          new ServerVersion(minServerVersion.asJava.asInstanceOf[java.util.List[Integer]])) >= 1
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

  implicit def observableToFuture[TResult](observable: Observable[TResult]): Future[List[TResult]] = {
    val promise = Promise[List[TResult]]()
    class FetchingObserver extends Observer[TResult]() {
      val results = new ListBuffer[TResult]()

      override def onSubscribe(s: Subscription): Unit = {
        s.request(Int.MaxValue)
      }

      override def onError(t: Throwable): Unit = {
        promise.failure(t)
      }

      override def onComplete(): Unit = {
        promise.success(results.toList)
      }

      override def onNext(t: TResult): Unit = results.append(t)
    }
    observable.subscribe(new FetchingObserver())
    promise.future
  }

  implicit def observableToFutureConcept[T](Observable: Observable[T]): FutureConcept[List[T]] = {
    val future: Future[List[T]] = Observable
    new FutureConcept[List[T]] {
      def eitherValue: Option[Either[Throwable, List[T]]] = {
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
