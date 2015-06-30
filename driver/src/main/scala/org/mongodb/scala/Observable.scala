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

import com.mongodb.async.client.{ Observable => JObservable, Observer => JObserver, Subscription => JSubscription }

import org.mongodb.scala.internal._

/**
 * A companion object for [[Observable]]
 */
object Observable {

  /**
   * Creates an Observable from an Iterable.
   *
   * Convenient for testing and or debugging.
   *
   * @param from the iterable to create the observable from
   * @tparam A the type of Iterable
   * @return an Observable that emits each item from the Iterable
   */
  def apply[A](from: Iterable[A]): Observable[A] = IterableObservable[A](from)

}

/**
 * A `Observable` represents a MongoDB operation.
 *
 * As such it is a provider of a potentially unbounded number of sequenced elements, publishing them according to the demand received
 * from its [[Observer]](s).
 *
 * @tparam T the type of element signaled.
 */
trait Observable[T] extends JObservable[T] {

  /**
   * Request `Observable` to start streaming data.
   *
   * This is a "factory method" and can be called multiple times, each time starting a new [[Subscription]].
   * Each `Subscription` will work for only a single [[Observer]].
   *
   * If the `Observable` rejects the subscription attempt or otherwise fails it will signal the error via [[Observer.onError]].
   *
   * @param observer the `Observer` that will consume signals from this `Observable`
   */
  def subscribe(observer: Observer[_ >: T]): Unit

  /**
   * Handles the automatic boxing of a Java `Observable` so it conforms to the interface.
   *
   * @note Users should not have to implement this method but rather use the Scala `Observable`.
   * @param observer the `Observer` that will consume signals from this `Observable`
   */
  override def subscribe(observer: JObserver[_ >: T]) = subscribe(BoxedObserver[T](observer))
}
