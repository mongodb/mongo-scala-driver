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

package org.mongodb.scala.model

import scala.collection.JavaConverters._

import org.mongodb.scala.bson.conversions.Bson
import com.mongodb.client.model.{ Aggregates => JAggregates }

/**
 * Builders for aggregation pipeline stages.
 *
 * @see [[http://docs.mongodb.org/manual/core/aggregation-pipeline/ Aggregation pipeline]]
 *
 * @since 1.0
 */
object Aggregates {
  /**
   * Creates a `\$match` pipeline stage for the specified filter
   *
   * @param filter the filter to match
   * @return the `\$match` pipeline stage
   * @see Filters
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/match/ \$match]]
   */
  def `match`(filter: Bson): Bson = JAggregates.`match`(filter) //scalastyle:ignore

  /**
   * Creates a `\$match` pipeline stage for the specified filter
   *
   * A friendly alias for the `match` method.
   *
   * @param filter the filter to match against
   * @return the `\$match` pipeline stage
   * @see Filters
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/match/ \$match]]
   */
  def filter(filter: Bson): Bson = `match`(filter) //scalastyle:ignore

  /**
   * Creates a `\$project` pipeline stage for the specified projection
   *
   * @param projection the projection
   * @return the `\$project` pipeline stage
   * @see Projections
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/project/ \$project]]
   */
  def project(projection: Bson): Bson = JAggregates.project(projection)

  /**
   * Creates a `\$sort` pipeline stage for the specified sort specification
   *
   * @param sort the sort specification
   * @return the `\$sort` pipeline stage
   * @see Sorts
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/sort/#sort-aggregation \$sort]]
   */
  def sort(sort: Bson): Bson = JAggregates.sort(sort)

  /**
   * Creates a `\$skip` pipeline stage
   *
   * @param skip the number of documents to skip
   * @return the `\$skip` pipeline stage
   * @see [[http://docs.mongodb.org/manual/ reference/operator/aggregation/skip/ \$skip]]
   */
  def skip(skip: Int): Bson = JAggregates.skip(skip)

  /**
   * Creates a `\$sample` pipeline stage with the specified sample size
   *
   * @param size the sample size
   * @return the `\$sample` pipeline stage
   * @since 1.1
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/sample/ \$sample]]
   */
  def sample(size: Int): Bson = JAggregates.sample(size)

  /**
   * Creates a `\$limit` pipeline stage for the specified filter
   *
   * @param limit the limit
   * @return the `\$limit` pipeline stage
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/limit/ \$limit]]
   */
  def limit(limit: Int): Bson = JAggregates.limit(limit)

  /**
   * Creates a `\$lookup` pipeline stage for the specified filter
   *
   * '''Note:'''Requires MongoDB 3.2 or greater
   * @param from the name of the collection in the same database to perform the join with.
   * @param localField specifies the field from the local collection to match values against.
   * @param foreignField specifies the field in the from collection to match values against.
   * @param as the name of the new array field to add to the input documents.
   * @return the `\$lookup` pipeline stage
   * @since 1.1
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/lookup/ \$lookup]]
   */
  def lookup(from: String, localField: String, foreignField: String, as: String): Bson =
    JAggregates.lookup(from, localField, foreignField, as)

  /**
   * Creates a `\$group` pipeline stage for the specified filter
   *
   * @param id the id expression for the group
   * @param fieldAccumulators zero or more field accumulator pairs
   * @tparam TExpression the expression type
   * @return the `\$group` pipeline stage
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/group/ \$group]]
   * @see [[http://docs.mongodb.org/manual/meta/aggregation-quick-reference/#aggregation-expressions Expressions]]
   */
  def group[TExpression](id: TExpression, fieldAccumulators: BsonField*): Bson = JAggregates.group(id, fieldAccumulators.asJava)

  /**
   * Creates a `\$unwind` pipeline stage for the specified field name, which must be prefixed by a `\$` sign.
   *
   * @param fieldName the field name, prefixed by a  `\$` sign
   * @return the `\$unwind` pipeline stage
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/unwind/ \$unwind]]
   */
  def unwind(fieldName: String): Bson = JAggregates.unwind(fieldName)

  /**
   * Creates a `\$unwind` pipeline stage for the specified field name, which must be prefixed by a `\$` sign.
   *
   * @param fieldName the field name, prefixed by a  `\$` sign
   * @return the `\$unwind` pipeline stage
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/unwind/ \$unwind]]
   * @since 1.1
   */
  def unwind(fieldName: String, unwindOptions: UnwindOptions): Bson = JAggregates.unwind(fieldName, unwindOptions)

  /**
   * Creates a `\$out` pipeline stage for the specified filter
   *
   * @param collectionName the collection name
   * @return the `\$out` pipeline stage
   * @see [[http://docs.mongodb.org/manual/reference/operator/aggregation/out/  \$out]]
   */
  def out(collectionName: String): Bson = JAggregates.out(collectionName)
}
