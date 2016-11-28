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

  def apply[A]() = {
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
  val results: mutable.ListBuffer[A] = mutable.ListBuffer[A]()
  var completed: Boolean = false

  override def onError(throwable: Throwable): Unit = {
    error = Some(throwable)
    delegate.onError(throwable)
  }

  override def onSubscribe(sub: Subscription): Unit = {
    subscription = Some(sub)
    delegate.onSubscribe(sub)
  }

  override def onComplete(): Unit = {
    completed = true
    delegate.onComplete()
  }

  override def onNext(result: A): Unit = {
    results.append(result)
    delegate.onNext(result)
  }
}
