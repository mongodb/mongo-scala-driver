/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala.internal

import scala.concurrent.ExecutionContext

import org.mongodb.scala.{Observable, Observer, Subscription}

private[scala] case class ExecutionContextObservable[T](context: ExecutionContext, original: Observable[T]) extends Observable[T] {

  override def subscribe(observer: Observer[_ >: T]): Unit = {
    original.subscribe(SubscriptionCheckingObserver(
      new Observer[T] {
        override def onError(throwable: Throwable): Unit = context.execute(new Runnable {
          override def run() = observer.onError(throwable)
        })

        override def onSubscribe(subscription: Subscription): Unit = context.execute(new Runnable {
          override def run() = observer.onSubscribe(subscription)
        })

        override def onComplete(): Unit = context.execute(new Runnable {
          override def run() = observer.onComplete()
        })

        override def onNext(tResult: T): Unit = context.execute(new Runnable {
          override def run() = observer.onNext(tResult)
        })
      }
    ))
  }
}

