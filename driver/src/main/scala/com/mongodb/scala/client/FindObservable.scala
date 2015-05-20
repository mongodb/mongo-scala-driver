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

import org.bson.conversions.Bson
import com.mongodb.CursorType
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.FindIterable

import com.mongodb.scala.client.ObservableHelper.observe

/**
 * Observable interface for Find.
 *
 * @param wrapped the underlying java FindObservable
 * @tparam T The type of the result.
 */
case class FindObservable[T](private val wrapped: FindIterable[T]) extends Observable[T] {
  /**
   * Helper to return a Observable limited to just the first result the query.
   *
   * **Note:** Sets limit in the background so only returns 1.
   *
   * @return a Observable which will return the first item
   */
  def first(): Observable[T] = observe(wrapped.first(_: SingleResultCallback[T]))

  /**
   * Sets the query filter to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
   * @param filter the filter, which may be null.
   * @return this
   */
  def filter(filter: Bson): FindObservable[T] = {
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
  def limit(limit: Int): FindObservable[T] = {
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
  def skip(skip: Int): FindObservable[T] = {
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
  def maxTime(duration: Duration): FindObservable[T] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Sets the query modifiers to apply to this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/query-modifier/ Query Modifiers]]
   * @param modifiers the query modifiers to apply, which may be null.
   * @return this
   */
  def modifiers(modifiers: Bson): FindObservable[T] = {
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
  def projection(projection: Bson): FindObservable[T] = {
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
  def sort(sort: Bson): FindObservable[T] = {
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
  def noCursorTimeout(noCursorTimeout: Boolean): FindObservable[T] = {
    wrapped.noCursorTimeout(noCursorTimeout)
    this
  }

  /**
   * Users should not set this under normal circumstances.
   *
   * @param oplogReplay if oplog replay is enabled
   * @return this
   */
  def oplogReplay(oplogReplay: Boolean): FindObservable[T] = {
    wrapped.oplogReplay(oplogReplay)
    this
  }

  /**
   * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
   *
   * @param partial if partial results for sharded clusters is enabled
   * @return this
   */
  def partial(partial: Boolean): FindObservable[T] = {
    wrapped.partial(partial)
    this
  }

  /**
   * Sets the cursor type.
   *
   * @param cursorType the cursor type
   * @return this
   */
  def cursorType(cursorType: CursorType): FindObservable[T] = {
    wrapped.cursorType(cursorType)
    this
  }

  override def subscribe(observer: Observer[_ >: T]): Unit = observe(wrapped).subscribe(observer)
}

