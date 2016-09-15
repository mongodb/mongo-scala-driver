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

import scala.concurrent.duration.Duration

import com.mongodb.CursorType
import com.mongodb.async.client.{FindIterable, MongoIterable}

import org.mongodb.scala.model.Collation
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class FindObservableSpec extends FlatSpec with Matchers with MockFactory {

  "FindObservable" should "have the same methods as the wrapped FindIterable" in {
    val mongoIterable: Set[String] = classOf[MongoIterable[Document]].getMethods.map(_.getName).toSet
    val wrapped = classOf[FindIterable[Document]].getMethods.map(_.getName).toSet -- mongoIterable
    val local = classOf[FindObservable[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[FindIterable[Document]]
    val Observable = FindObservable(wrapper)

    val filter = Document("a" -> 1)
    val duration = Duration(1, TimeUnit.SECONDS)
    val maxDuration = Duration(10, TimeUnit.SECONDS)
    val modifiers = Document("mod" -> 1)
    val projection = Document("proj" -> 1)
    val sort = Document("sort" -> 1)
    val collation = Collation.builder().locale("en").build()

    val observer = new Observer[Document]() {
      override def onError(throwable: Throwable): Unit = {}
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
      override def onComplete(): Unit = {}
      override def onNext(doc: Document): Unit = {}
    }

    wrapper.expects('first)(*).once()
    wrapper.expects('filter)(filter).once()
    wrapper.expects('maxTime)(duration.toMillis, TimeUnit.MILLISECONDS).once()
    wrapper.expects('maxAwaitTime)(maxDuration.toMillis, TimeUnit.MILLISECONDS).once()
    wrapper.expects('limit)(1).once()
    wrapper.expects('skip)(1).once()
    wrapper.expects('modifiers)(modifiers).once()
    wrapper.expects('projection)(projection).once()
    wrapper.expects('sort)(sort).once()
    wrapper.expects('noCursorTimeout)(true).once()
    wrapper.expects('oplogReplay)(true).once()
    wrapper.expects('partial)(true).once()
    wrapper.expects('cursorType)(CursorType.NonTailable).once()
    wrapper.expects('collation)(collation).once()
    wrapper.expects('batchSize)(Int.MaxValue).once()
    wrapper.expects('batchCursor)(*).once()

    Observable.first().subscribe(observer)
    Observable.filter(filter)
    Observable.maxTime(duration)
    Observable.maxAwaitTime(maxDuration)
    Observable.limit(1)
    Observable.skip(1)
    Observable.modifiers(modifiers)
    Observable.projection(projection)
    Observable.sort(sort)
    Observable.noCursorTimeout(true)
    Observable.oplogReplay(true)
    Observable.partial(true)
    Observable.cursorType(CursorType.NonTailable)
    Observable.collation(collation)
    Observable.subscribe(observer)
  }
}
