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

package rxScala

import java.util.concurrent.atomic.AtomicBoolean

import scala.language.implicitConversions

import org.mongodb.scala._
import rx.lang.{ scala => rx }

object Implicits {

  implicit def observableToRxObservable[T](observable: Observable[T]): rx.Observable[T] =
    rx.Observable[T]((subscriber: rx.Subscriber[_ >: T]) => ObservableToProducer[T](observable, subscriber).subscribe())

  private case class ObservableToProducer[T](observable: Observable[T], rxSubscriber: rx.Subscriber[_ >: T]) extends rx.Producer {
    @volatile
    private var subscription: Option[Subscription] = None

    def subscribe() = {
      observable.subscribe(new Observer[T]() {
        override def onSubscribe(s: Subscription) {
          subscription = Some(s)
          rxSubscriber.add(new rx.Subscription() {
            private final val unsubscribed: AtomicBoolean = new AtomicBoolean

            override def unsubscribe() = {
              if (!unsubscribed.getAndSet(true)) {
                subscription match {
                  case Some(sub) => sub.unsubscribe()
                  case None =>
                }
              }
            }

            override def isUnsubscribed: Boolean = subscription match {
              case Some(sub) => sub.isUnsubscribed
              case None => true
            }
          })
        }

        override def onNext(tResult: T) {
          if (isSubscribed) {
            rxSubscriber.onNext(tResult)
          }
        }

        override def onError(t: Throwable) {
          if (isSubscribed) {
            rxSubscriber.onError(t)
          }
        }

        def onComplete() {
          if (isSubscribed) {
            rxSubscriber.onCompleted()
          }
        }
      })
      rxSubscriber.setProducer(this)
      rxSubscriber.onStart()
      this
    }

    override def request(n: Long) {
      if (isSubscribed) {
        subscription.get.request(n)
      }
    }

    private def isSubscribed: Boolean = !rxSubscriber.isUnsubscribed
  }
}
