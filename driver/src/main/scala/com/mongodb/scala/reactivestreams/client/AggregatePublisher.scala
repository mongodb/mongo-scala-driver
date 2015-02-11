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

import org.reactivestreams.{ Subscriber, Publisher }
import com.mongodb.reactivestreams.client.{ AggregatePublisher => JAggregatePublisher }
import scala.concurrent.duration.Duration

/**
 * Publisher for aggregate
 *
 * @param wrapped the underlying java AggregatePublisher
 * @tparam T The type of the result.
 */
case class AggregatePublisher[T](private val wrapped: JAggregatePublisher[T]) extends Publisher[T] {

  /**
   * Enables writing to temporary files. A null value indicates that it's unspecified.
   *
   * [[http://docs.mongodb.org/manual/reference/command/aggregate/ Aggregation]]
   *
   * @param allowDiskUse true if writing to temporary files is enabled
   * @return this
   */
  def allowDiskUse(allowDiskUse: Boolean): AggregatePublisher[T] = {
    wrapped.allowDiskUse(allowDiskUse)
    this
  }

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return this
   */
  def maxTime(duration: Duration): AggregatePublisher[T] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Sets whether the server should use a cursor to return results.
   *
   * [[http://docs.mongodb.org/manual/reference/command/aggregate/ Aggregation]]
   * @param useCursor whether the server should use a cursor to return results
   * @return this
   */
  def useCursor(useCursor: Boolean): AggregatePublisher[T] = {
    wrapped.useCursor(useCursor)
    this
  }

  /**
   * Aggregates documents according to the specified aggregation pipeline, which must end with a `\$out` stage.
   *
   * [[http://docs.mongodb.org/manual/aggregation/ Aggregation]]
   * @return a publisher with a single element indicating when the operation has completed
   */
  def toCollection(): Publisher[Void] = wrapped.toCollection()

  override def subscribe(subscriber: Subscriber[_ >: T]): Unit = wrapped.subscribe(subscriber)
}
