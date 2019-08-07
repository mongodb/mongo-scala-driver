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

import com.mongodb.async.client.{MapReduceIterable, MongoIterable}
import com.mongodb.client.model.MapReduceAction

import org.mongodb.scala.model.Collation
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

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
    val observable = MapReduceObservable(wrapper)

    val filter = Document("a" -> 1)
    val duration = Duration(1, TimeUnit.SECONDS)
    val sort = Document("sort" -> 1)
    val scope = Document("mod" -> 1)
    val collation = Collation.builder().locale("en").build()
    val batchSize = 10

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
    wrapper.expects('bypassDocumentValidation)(true).once()
    wrapper.expects('collation)(collation).once()
    wrapper.expects('toCollection)(*).once()
    wrapper.expects('getBatchSize)().once()
    wrapper.expects('batchSize)(Int.MaxValue).once()
    wrapper.expects('batchCursor)(*).once()

    observable.filter(filter)
    observable.scope(scope)
    observable.sort(sort)
    observable.limit(1)
    observable.maxTime(duration)
    observable.collectionName("collectionName")
    observable.databaseName("databaseName")
    observable.finalizeFunction("final")
    observable.action(MapReduceAction.REPLACE)
    observable.jsMode(true)
    observable.verbose(true)
    observable.sharded(true)
    observable.nonAtomic(true)
    observable.bypassDocumentValidation(true)
    observable.collation(collation)
    observable.toCollection().subscribe(observer[Completed])
    observable.subscribe(observer[Document])

    wrapper.expects('batchSize)(batchSize).once()
    wrapper.expects('getBatchSize)().once()

    observable.batchSize(batchSize)
    observable.subscribe(observer[Document])
  }
}
