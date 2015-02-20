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

import java.util.concurrent.TimeUnit

import com.mongodb.client.model.MapReduceAction
import com.mongodb.reactivestreams.client.{ MapReducePublisher => JMapReducePublisher }
import org.bson.Document
import org.reactivestreams.Subscriber
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.duration.Duration

class MapReducePublisherSpec extends FlatSpec with Matchers with MockFactory {

  "MapReducePublisher" should "have the same methods as the wrapped MapReducePublisher" in {
    val wrapped = classOf[JMapReducePublisher[Document]].getMethods.map(_.getName).toSet
    val local = classOf[MapReducePublisher[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[JMapReducePublisher[Document]]
    val publisher = MapReducePublisher(wrapper)

    val filter = new Document("a", 1)
    val duration = Duration(1, TimeUnit.SECONDS)
    val sort = new Document("sort", 1)
    val scope = new Document("mod", 1)
    val subscriber = stub[Subscriber[Document]]

    (wrapper.filter _).expects(filter).once()
    (wrapper.scope _).expects(scope).once()
    (wrapper.sort _).expects(sort).once()
    (wrapper.limit _).expects(1).once()
    (wrapper.maxTime(_: Long, _: TimeUnit)).expects(duration.toMillis, TimeUnit.MILLISECONDS).once()
    (wrapper.collectionName _).expects("collectionName").once()
    (wrapper.databaseName _).expects("databaseName").once()
    (wrapper.finalizeFunction _).expects("final").once()
    (wrapper.action _).expects(MapReduceAction.REPLACE).once()
    (wrapper.jsMode _).expects(true).once()
    (wrapper.verbose _).expects(true).once()
    (wrapper.sharded _).expects(true).once()
    (wrapper.nonAtomic _).expects(true).once()

    (wrapper.toCollection _).expects().once()
    (wrapper.subscribe _).expects(subscriber).once()

    publisher.filter(filter)
    publisher.scope(scope)
    publisher.sort(sort)
    publisher.limit(1)
    publisher.maxTime(duration)
    publisher.collectionName("collectionName")
    publisher.databaseName("databaseName")
    publisher.finalizeFunction("final")
    publisher.action(MapReduceAction.REPLACE)
    publisher.jsMode(true)
    publisher.verbose(true)
    publisher.sharded(true)
    publisher.nonAtomic(true)
    publisher.toCollection()
    publisher.subscribe(subscriber)
  }
}
