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

package com.mongodb.scala.reactivestreams.client

import java.util.concurrent.TimeUnit

import org.bson.conversions.Bson
import org.reactivestreams.{ Subscriber, Publisher }
import com.mongodb.reactivestreams.client.{ DistinctPublisher => JDistinctPublisher }
import scala.concurrent.duration.Duration

/**
 * Publisher for distinct
 *
 * @param wrapped the underlying java DistinctPublisher
 * @tparam T The type of the result.
 */
case class DistinctPublisher[T](private val wrapped: JDistinctPublisher[T]) extends Publisher[T] {
  /**
   * Sets the query filter to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
   * @param filter the filter, which may be null.
   * @return this
   */
  def filter(filter: Bson): DistinctPublisher[T] = {
    wrapped.filter(filter)
    this
  }

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return this
   */
  def maxTime(duration: Duration): DistinctPublisher[T] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  override def subscribe(subscriber: Subscriber[_ >: T]): Unit = wrapped.subscribe(subscriber)
}
