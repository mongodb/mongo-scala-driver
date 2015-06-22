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

import com.mongodb.async.client.{ AggregateIterable, MongoIterable }

import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class AggregateObservableSpec extends FlatSpec with Matchers with MockFactory {

  def observer[T] = new Observer[T]() {
    override def onError(throwable: Throwable): Unit = {}
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
    override def onComplete(): Unit = {}
    override def onNext(doc: T): Unit = {}
  }

  "AggregateObservable" should "have the same methods as the wrapped AggregateObservable" in {
    val mongoIterable: Set[String] = classOf[MongoIterable[Document]].getMethods.map(_.getName).toSet
    val wrapped: Set[String] = classOf[AggregateIterable[Document]].getMethods.map(_.getName).toSet -- mongoIterable
    val local = classOf[AggregateObservable[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[AggregateIterable[Document]]
    val Observable = AggregateObservable(wrapper)

    val duration = Duration(1, TimeUnit.SECONDS)

    wrapper.expects('allowDiskUse)(java.lang.Boolean.TRUE).once()
    wrapper.expects('useCursor)(java.lang.Boolean.TRUE).once()
    wrapper.expects('maxTime)(duration.toMillis, TimeUnit.MILLISECONDS).once()
    wrapper.expects('toCollection)(*).once()
    wrapper.expects('batchSize)(Int.MaxValue).once()
    wrapper.expects('batchCursor)(*).once()

    Observable.allowDiskUse(true)
    Observable.useCursor(true)
    Observable.maxTime(duration)
    Observable.toCollection().subscribe(observer[Void])
    Observable.subscribe(observer[Document])
  }
}
