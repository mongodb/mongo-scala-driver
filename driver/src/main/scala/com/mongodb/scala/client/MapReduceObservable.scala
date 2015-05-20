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
import com.mongodb.client.model.MapReduceAction
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MapReduceIterable

import com.mongodb.scala.client.ObservableHelper.observe

case class MapReduceObservable[T](wrapped: MapReduceIterable[T]) extends Observable[T] {
  /**
   * Sets the collectionName for the output of the MapReduce
   *
   * <p>The default action is replace the collection if it exists, to change this use {@link #action}.</p>
   *
   * @param collectionName the name of the collection that you want the map-reduce operation to write its output.
   * @return this
   */
  def collectionName(collectionName: String): MapReduceObservable[T] = {
    wrapped.collectionName(collectionName)
    this
  }

  /**
   * Sets the JavaScript function that follows the reduce method and modifies the output.
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce/#mapreduce-finalize-cmd Requirements for the finalize Function]]
   * @param finalizeFunction the JavaScript function that follows the reduce method and modifies the output.
   * @return this
   */
  def finalizeFunction(finalizeFunction: String): MapReduceObservable[T] = {
    wrapped.finalizeFunction(finalizeFunction)
    this
  }

  /**
   * Sets the global variables that are accessible in the map, reduce and finalize functions.
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce mapReduce]]
   * @param scope the global variables that are accessible in the map, reduce and finalize functions.
   * @return this
   */
  def scope(scope: Bson): MapReduceObservable[T] = {
    wrapped.scope(scope)
    this
  }

  /**
   * Sets the sort criteria to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/cursor.sort/ Sort]]
   * @param sort the sort criteria, which may be null.
   * @return this
   */
  def sort(sort: Bson): MapReduceObservable[T] = {
    wrapped.sort(sort)
    this
  }

  /**
   * Sets the query filter to apply to the query.
   *
   * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
   * @param filter the filter to apply to the query.
   * @return this
   */
  def filter(filter: Bson): MapReduceObservable[T] = {
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
  def limit(limit: Int): MapReduceObservable[T] = {
    wrapped.limit(limit)
    this
  }

  /**
   * Sets the flag that specifies whether to convert intermediate data into BSON format between the execution of the map and reduce
   * functions. Defaults to false.
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce mapReduce]]
   * @param jsMode the flag that specifies whether to convert intermediate data into BSON format between the execution of the map and
   *               reduce functions
   * @return jsMode
   */
  def jsMode(jsMode: Boolean): MapReduceObservable[T] = {
    wrapped.jsMode(jsMode)
    this
  }

  /**
   * Sets whether to include the timing information in the result information.
   *
   * @param verbose whether to include the timing information in the result information.
   * @return this
   */
  def verbose(verbose: Boolean): MapReduceObservable[T] = {
    wrapped.verbose(verbose)
    this
  }

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
   * @param duration the duration
   * @return this
   */
  def maxTime(duration: Duration): MapReduceObservable[T] = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Specify the `MapReduceAction` to be used when writing to a collection.
   *
   * @param action an { @link com.mongodb.client.model.MapReduceAction} to perform on the collection
   * @return this
   */
  def action(action: MapReduceAction): MapReduceObservable[T] = {
    wrapped.action(action)
    this
  }

  /**
   * Sets the name of the database to output into.
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce/#output-to-a-collection-with-an-action output with an action]]
   * @param databaseName the name of the database to output into.
   * @return this
   */
  def databaseName(databaseName: String): MapReduceObservable[T] = {
    wrapped.databaseName(databaseName)
    this
  }

  /**
   * Sets if the output database is sharded
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce/#output-to-a-collection-with-an-action output with an action]]
   * @param sharded if the output database is sharded
   * @return this
   */
  def sharded(sharded: Boolean): MapReduceObservable[T] = {
    wrapped.sharded(sharded)
    this
  }

  /**
   * Sets if the post-processing step will prevent MongoDB from locking the database.
   *
   * Valid only with the `MapReduceAction.MERGE` or `MapReduceAction.REDUCE` actions.
   *
   * [[http://docs.mongodb.org/manual/reference/command/mapReduce/#output-to-a-collection-with-an-action output with an action]]
   * @param nonAtomic if the post-processing step will prevent MongoDB from locking the database.
   * @return this
   */
  def nonAtomic(nonAtomic: Boolean): MapReduceObservable[T] = {
    wrapped.nonAtomic(nonAtomic)
    this
  }

  /**
   * Aggregates documents to a collection according to the specified map-reduce function with the given options, which must specify a
   * non-inline result.
   *
   * @return a Observable with a single element indicating when the operation has completed
   * [[http://docs.mongodb.org/manual/aggregation/ Aggregation]]
   */
  def toCollection(): Observable[Void] = observe(wrapped.toCollection(_: SingleResultCallback[Void]))

  override def subscribe(observer: Observer[_ >: T]): Unit = observe(wrapped).subscribe(observer)
}
