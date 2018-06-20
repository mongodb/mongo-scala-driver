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

package org.mongodb.scala.internal

import scala.collection.mutable

import org.mongodb.scala.{Observer, Subscription}

object TestObserver {

  def apply[A](): TestObserver[A] = {
    new TestObserver[A](new Observer[A] {
      override def onError(throwable: Throwable): Unit = {}

      override def onSubscribe(subscription: Subscription): Unit = {}

      override def onComplete(): Unit = {}

      override def onNext(tResult: A): Unit = {}
    })
  }

}

case class TestObserver[A](delegate: Observer[A]) extends Observer[A] {
  var subscription: Option[Subscription] = None
  var error: Option[Throwable] = None
  var completed: Boolean = false
  var terminated: Boolean = false
  val results: mutable.ListBuffer[A] = mutable.ListBuffer[A]()

  override def onError(throwable: Throwable): Unit = {
    require(!terminated, "onError called after the observer has already been terminated")
    terminated = true
    error = Some(throwable)
    delegate.onError(throwable)
  }

  override def onSubscribe(sub: Subscription): Unit = {
    require(subscription.isEmpty, "bserver already subscribed to")
    subscription = Some(sub)
    delegate.onSubscribe(sub)
  }

  override def onComplete(): Unit = {
    require(!terminated, "onComplete called after the observer has already been terminated")
    terminated = true
    delegate.onComplete()
    completed = true
  }

  override def onNext(result: A): Unit = {
    require(!terminated, "onNext called after the observer has already been terminated")
    this.synchronized {
      results.append(result)
    }
    delegate.onNext(result)
  }
}
