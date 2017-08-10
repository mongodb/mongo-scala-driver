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

import com.mongodb.CursorType
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.FindIterable

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.internal.ObservableHelper._
import org.mongodb.scala.model.Collation

/**
 * Observable interface for Find.
 *
 * @param wrapped the underlying java FindObservable
 * @tparam TResult The type of the result.
 * @since 1.0
 */
case class FindObservable[TResult](private val wrapped: FindIterable[TResult]) extends Observable[TResult] {
  /**
   * Helper to return a Observable limited to just the first result the query.
   *
   * **Note:** Sets limit in the background so only returns 1.
   *
   * @return a Observable which will return the first item
   */
  def first(): SingleObservable[TResult] = observe(wrapped.first(_: SingleResultCallback[TResult]))

  /**
   * Sets the query filter to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
   * @param filter the filter, which may be null.
   * @return this
   */
  def filter(filter: Bson): FindObservable[TResult] = {
    wrapped.filter(filter)
    this
  }

  /**
   * Sets the limit to apply.
   *
   * [[http://docs.mongodb.org/manual/reference/method/cursor.limit/#cursor.limit Limit]]
   * @param limit the limit, which may be null
   * @return this
   */
  def limit(limit: Int): FindObservable[TResult] = {
    wrapped.limit(limit)
    this
  }

  /**
   * Sets the number of documents to skip.
   *
   * [[http://docs.mongodb.org/manual/reference/method/cursor.skip/#cursor.skip Skip]]
   * @param skip the number of documents to skip
   * @return this
   */
  def skip(skip: Int): FindObservable[TResult] = {
    wrapped.skip(skip)
    this
  }

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return this
   */
  def maxTime(duration: Duration): FindObservable[TResult] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * The maximum amount of time for the server to wait on new documents to satisfy a tailable cursor
   * query. This only applies to a TAILABLE_AWAIT cursor. When the cursor is not a TAILABLE_AWAIT cursor,
   * this option is ignored.
   *
   * On servers &gt;= 3.2, this option will be specified on the getMore command as "maxTimeMS". The default
   * is no value: no "maxTimeMS" is sent to the server with the getMore command.
   *
   * On servers &lt; 3.2, this option is ignored, and indicates that the driver should respect the server's default value
   *
   * A zero value will be ignored.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return the maximum await execution time in the given time unit
   * @since 1.1
   */
  def maxAwaitTime(duration: Duration): FindObservable[TResult] = {
    wrapped.maxAwaitTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Sets the query modifiers to apply to this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/query-modifier/ Query Modifiers]]
   * @param modifiers the query modifiers to apply, which may be null.
   * @return this
   */
  def modifiers(modifiers: Bson): FindObservable[TResult] = {
    wrapped.modifiers(modifiers)
    this
  }

  /**
   * Sets a document describing the fields to return for all matching documents.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Projection]]
   * @param projection the project document, which may be null.
   * @return this
   */
  def projection(projection: Bson): FindObservable[TResult] = {
    wrapped.projection(projection)
    this
  }

  /**
   * Sets the sort criteria to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/cursor.sort/ Sort]]
   * @param sort the sort criteria, which may be null.
   * @return this
   */
  def sort(sort: Bson): FindObservable[TResult] = {
    wrapped.sort(sort)
    this
  }

  /**
   * The server normally times out idle cursors after an inactivity period (10 minutes)
   * to prevent excess memory use. Set this option to prevent that.
   *
   * @param noCursorTimeout true if cursor timeout is disabled
   * @return this
   */
  def noCursorTimeout(noCursorTimeout: Boolean): FindObservable[TResult] = {
    wrapped.noCursorTimeout(noCursorTimeout)
    this
  }

  /**
   * Users should not set this under normal circumstances.
   *
   * @param oplogReplay if oplog replay is enabled
   * @return this
   */
  def oplogReplay(oplogReplay: Boolean): FindObservable[TResult] = {
    wrapped.oplogReplay(oplogReplay)
    this
  }

  /**
   * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
   *
   * @param partial if partial results for sharded clusters is enabled
   * @return this
   */
  def partial(partial: Boolean): FindObservable[TResult] = {
    wrapped.partial(partial)
    this
  }

  /**
   * Sets the cursor type.
   *
   * @param cursorType the cursor type
   * @return this
   */
  def cursorType(cursorType: CursorType): FindObservable[TResult] = {
    wrapped.cursorType(cursorType)
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
  def collation(collation: Collation): FindObservable[TResult] = {
    wrapped.collation(collation)
    this
  }

  /**
   * Sets the batch size.
   * @param batchSize the batch size.
   * @since 2.1.0
   * @return this
   * @note Specifying 1 or a negative number is analogous to using the limit() method.
   */
  def batchSize(batchSize: Int): FindObservable[TResult] = {
    wrapped.batchSize(batchSize)
    this
  }

  override def subscribe(observer: Observer[_ >: TResult]): Unit = observe(wrapped).subscribe(observer)
}
