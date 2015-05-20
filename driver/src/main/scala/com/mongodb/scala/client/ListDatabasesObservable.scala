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

package com.mongodb.scala.client

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import com.mongodb.async.client.ListDatabasesIterable

import com.mongodb.scala.client.ObservableHelper.observe

/**
 * Observable interface for ListDatabases.
 *
 * @param wrapped the underlying java ListDatabasesObservable
 * @tparam T The type of the result.
 */
case class ListDatabasesObservable[T](wrapped: ListDatabasesIterable[T]) extends Observable[T] {

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return this
   */
  def maxTime(duration: Duration): ListDatabasesObservable[T] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  override def subscribe(observer: Observer[_ >: T]): Unit = observe(wrapped).subscribe(observer)
}

