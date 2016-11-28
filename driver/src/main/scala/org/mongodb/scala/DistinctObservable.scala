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

import com.mongodb.async.client.DistinctIterable

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.internal.ObservableHelper._
import org.mongodb.scala.model.Collation

/**
 * Observable for distinct
 *
 * @param wrapped the underlying java DistinctObservable
 * @tparam TResult The type of the result.
 * @since 1.0
 */
case class DistinctObservable[TResult](private val wrapped: DistinctIterable[TResult]) extends Observable[TResult] {
  /**
   * Sets the query filter to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
   * @param filter the filter, which may be null.
   * @return this
   */
  def filter(filter: Bson): DistinctObservable[TResult] = {
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
  def maxTime(duration: Duration): DistinctObservable[TResult] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Sets the collation options
   *
   * @param collation the collation options to use
   * @return this
   * @since 1.2
   * @note A null value represents the server default.
   * @note Requires MongoDB 3.4 or greater
   */
  def collation(collation: Collation): DistinctObservable[TResult] = {
    wrapped.collation(collation)
    this
  }

  override def subscribe(observer: Observer[_ >: TResult]): Unit = observe(wrapped).subscribe(observer)
}
