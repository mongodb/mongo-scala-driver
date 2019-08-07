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

import java.util.concurrent.TimeUnit

import com.mongodb.async.client.{ChangeStreamIterable, MongoIterable}
import org.mongodb.scala.bson.BsonTimestamp
import org.mongodb.scala.model.Collation
import org.mongodb.scala.model.changestream.FullDocument
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration

class ChangeStreamObservableSpec extends FlatSpec with Matchers with MockFactory {

  "ChangeStreamObservable" should "have the same methods as the wrapped ChangeStreamObservable" in {
    val mongoIterable: Set[String] = classOf[MongoIterable[Document]].getMethods.map(_.getName).toSet
    val wrapped: Set[String] = classOf[ChangeStreamIterable[Document]].getMethods.map(_.getName).toSet -- mongoIterable
    val local = classOf[ChangeStreamObservable[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[ChangeStreamIterable[Document]]
    val observable = ChangeStreamObservable[Document](wrapper)

    val duration = Duration(1, TimeUnit.SECONDS)
    val resumeToken = Document()
    val fullDocument = FullDocument.DEFAULT
    val startAtTime = BsonTimestamp()
    val collation = Collation.builder().locale("en").build()
    val batchSize = 10

    wrapper.expects('batchSize)(batchSize).once()
    wrapper.expects('fullDocument)(fullDocument).once()
    wrapper.expects('resumeAfter)(resumeToken.underlying).once()
    wrapper.expects('startAfter)(resumeToken.underlying).once()
    wrapper.expects('startAtOperationTime)(startAtTime).once()
    wrapper.expects('maxAwaitTime)(duration.toMillis, TimeUnit.MILLISECONDS).once()
    wrapper.expects('collation)(collation).once()
    wrapper.expects('withDocumentClass)(classOf[Int]).once()

    observable.batchSize(batchSize)
    observable.fullDocument(fullDocument)
    observable.resumeAfter(resumeToken)
    observable.startAfter(resumeToken)
    observable.startAtOperationTime(startAtTime)
    observable.maxAwaitTime(duration)
    observable.collation(collation)
    observable.withDocumentClass(classOf[Int])
  }
}
