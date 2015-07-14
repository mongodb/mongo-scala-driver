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

import com.mongodb.client.model.MapReduceAction
import com.mongodb.async.client.{ MapReduceIterable, MongoIterable }

import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MapReduceObservableSpec extends FlatSpec with Matchers with MockFactory {

  def observer[T] = new Observer[T]() {
    override def onError(throwable: Throwable): Unit = {}
    override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)
    override def onComplete(): Unit = {}
    override def onNext(doc: T): Unit = {}
  }

  "MapReduceObservable" should "have the same methods as the wrapped MapReduceObservable" in {
    val mongoIterable: Set[String] = classOf[MongoIterable[Document]].getMethods.map(_.getName).toSet
    val wrapped = classOf[MapReduceIterable[Document]].getMethods.map(_.getName).toSet -- mongoIterable
    val local = classOf[MapReduceObservable[Document]].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods" in {
    val wrapper = mock[MapReduceIterable[Document]]
    val Observable = MapReduceObservable(wrapper)

    val filter = Document("a" -> 1)
    val duration = Duration(1, TimeUnit.SECONDS)
    val sort = Document("sort" -> 1)
    val scope = Document("mod" -> 1)

    wrapper.expects('filter)(filter).once()
    wrapper.expects('scope)(scope).once()
    wrapper.expects('sort)(sort).once()
    wrapper.expects('limit)(1).once()
    wrapper.expects('maxTime)(duration.toMillis, TimeUnit.MILLISECONDS).once()
    wrapper.expects('collectionName)("collectionName").once()
    wrapper.expects('databaseName)("databaseName").once()
    wrapper.expects('finalizeFunction)("final").once()
    wrapper.expects('action)(MapReduceAction.REPLACE).once()
    wrapper.expects('jsMode)(true).once()
    wrapper.expects('verbose)(true).once()
    wrapper.expects('sharded)(true).once()
    wrapper.expects('nonAtomic)(true).once()
    wrapper.expects('toCollection)(*).once()
    wrapper.expects('batchSize)(Int.MaxValue).once()
    wrapper.expects('batchCursor)(*).once()

    Observable.filter(filter)
    Observable.scope(scope)
    Observable.sort(sort)
    Observable.limit(1)
    Observable.maxTime(duration)
    Observable.collectionName("collectionName")
    Observable.databaseName("databaseName")
    Observable.finalizeFunction("final")
    Observable.action(MapReduceAction.REPLACE)
    Observable.jsMode(true)
    Observable.verbose(true)
    Observable.sharded(true)
    Observable.nonAtomic(true)
    Observable.toCollection().subscribe(observer[Completed])
    Observable.subscribe(observer[Document])
  }
}
