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

import com.mongodb.reactivestreams.client.{ ListDatabasesPublisher => JListDatabasesPublisher }
import org.bson.Document
import org.reactivestreams.Subscriber
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.duration.Duration

class ListDatabasesPublisherSpec extends FlatSpec with Matchers with MockFactory {

  "ListDatabasesPublisher" should "have the same methods as the wrapped ListDatabasesPublisher" in {
    val wrapped = classOf[JListDatabasesPublisher[Document]].getMethods.map(_.getName).toSet
    val local = classOf[ListDatabasesPublisher[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[JListDatabasesPublisher[Document]]
    val publisher = ListDatabasesPublisher(wrapper)

    val duration = Duration(1, TimeUnit.SECONDS)
    val subscriber = stub[Subscriber[Document]]

    (wrapper.maxTime(_: Long, _: TimeUnit)).expects(duration.toMillis, TimeUnit.MILLISECONDS).once()
    (wrapper.subscribe _).expects(subscriber).once()

    publisher.maxTime(duration)
    publisher.subscribe(subscriber)
  }
}
