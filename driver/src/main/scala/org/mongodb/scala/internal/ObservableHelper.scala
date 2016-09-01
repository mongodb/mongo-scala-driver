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

import java.util

import com.mongodb.Block
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{ MongoIterable, Observables }

import org.mongodb.scala._

/**
 * A helper to pass in Scala partial functions to the Observables helper
 */
private[scala] object ObservableHelper {

  def observe[T](mongoIterable: MongoIterable[T]): Observable[T] = Observables.observe(mongoIterable)

  def observe[T](block: (SingleResultCallback[T]) => Unit): Observable[T] =
    Observables.observe(new Block[SingleResultCallback[T]] {
      override def apply(callback: SingleResultCallback[T]): Unit = block(callback)
    })

  def observeCompleted(block: (SingleResultCallback[Void]) => Unit): Observable[Completed] =
    Observables.observe(new Block[SingleResultCallback[Completed]] {
      override def apply(callback: SingleResultCallback[Completed]): Unit =
        block(new SingleResultCallback[Void]() {
          def onResult(result: Void, t: Throwable): Unit =
            callback.onResult(Completed(), t)
        })
    })

  def observeLong(block: (SingleResultCallback[java.lang.Long]) => Unit): Observable[Long] =
    ScalaObservable(Observables.observe(new Block[SingleResultCallback[java.lang.Long]] {
      override def apply(callback: SingleResultCallback[java.lang.Long]): Unit = block(callback)
    })).map(result => result.longValue())

  def observeInt(block: (SingleResultCallback[java.lang.Integer]) => Unit): Observable[Int] =
    ScalaObservable(Observables.observe(new Block[SingleResultCallback[java.lang.Integer]] {
      override def apply(callback: SingleResultCallback[java.lang.Integer]): Unit = block(callback)
    })).map(result => result.intValue())

  def observeAndFlatten[T](block: (SingleResultCallback[util.List[T]]) => Unit): Observable[T] =
    Observables.observeAndFlatten(new Block[SingleResultCallback[util.List[T]]] {
      override def apply(callback: SingleResultCallback[util.List[T]]): Unit = block(callback)
    })
}
