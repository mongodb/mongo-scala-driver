/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.scala.reactivestreams.client

import java.util.concurrent.TimeUnit

import com.mongodb.CursorType
import com.mongodb.reactivestreams.client.{ FindPublisher => JFindPublisher }
import org.bson.Document
import org.reactivestreams.Subscriber
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.duration.Duration

class FindPublisherSpec extends FlatSpec with Matchers with MockFactory {

  "FindPublisher" should "have the same methods as the wrapped FindPublisher" in {
    val wrapped = classOf[JFindPublisher[Document]].getMethods.map(_.getName).toSet
    val local = classOf[FindPublisher[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[JFindPublisher[Document]]
    val publisher = FindPublisher(wrapper)

    val filter = new Document("a", 1)
    val duration = Duration(1, TimeUnit.SECONDS)
    val modifiers = new Document("mod", 1)
    val projection = new Document("proj", 1)
    val sort = new Document("sort", 1)
    val subscriber = stub[Subscriber[Document]]

    (wrapper.first _).expects().once()
    (wrapper.filter _).expects(filter).once()
    (wrapper.maxTime(_: Long, _: TimeUnit)).expects(duration.toMillis, TimeUnit.MILLISECONDS).once()
    (wrapper.limit _).expects(1).once()
    (wrapper.skip _).expects(1).once()
    (wrapper.modifiers _).expects(modifiers).once()
    (wrapper.projection _).expects(projection).once()
    (wrapper.sort _).expects(sort).once()
    (wrapper.noCursorTimeout _).expects(true).once()
    (wrapper.oplogReplay _).expects(true).once()
    (wrapper.partial _).expects(true).once()
    (wrapper.cursorType _).expects(CursorType.NonTailable).once()
    (wrapper.subscribe _).expects(subscriber).once()

    publisher.first()
    publisher.filter(filter)
    publisher.maxTime(duration)
    publisher.limit(1)
    publisher.skip(1)
    publisher.modifiers(modifiers)
    publisher.projection(projection)
    publisher.sort(sort)
    publisher.noCursorTimeout(true)
    publisher.oplogReplay(true)
    publisher.partial(true)
    publisher.cursorType(CursorType.NonTailable)
    publisher.subscribe(subscriber)
  }
}
