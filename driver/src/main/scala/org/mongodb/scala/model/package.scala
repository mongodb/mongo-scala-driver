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

import org.bson.conversions.Bson

/**
 * The model package containing models and options that help describe `MongoCollection` operations
 */
package object model {

  /**
   * A representation of a BSON document field whose value is another BSON document.
   */
  type BsonField = com.mongodb.client.model.BsonField

  object BsonField {
    def apply(name: String, value: Bson): BsonField = {
      new com.mongodb.client.model.BsonField(name, value)
    }
  }

  /**
   * The options to apply to a bulk write.
   */
  type BulkWriteOptions = com.mongodb.client.model.BulkWriteOptions

  object BulkWriteOptions {
    def apply(): BulkWriteOptions = new com.mongodb.client.model.BulkWriteOptions()
  }

  /**
   * The options to apply to a count operation.
   */
  type CountOptions = com.mongodb.client.model.CountOptions

  object CountOptions {
    def apply(): CountOptions = new com.mongodb.client.model.CountOptions()
  }

  /**
   * Options for creating a collection
   *
   */
  type CreateCollectionOptions = com.mongodb.client.model.CreateCollectionOptions

  object CreateCollectionOptions {
    def apply(): CreateCollectionOptions = new com.mongodb.client.model.CreateCollectionOptions()
  }

  /**
   * A model describing the removal of all documents matching the query filter.
   *
   * @tparam TResult the type of document to update.  In practice this doesn't actually apply to updates but is here for consistency with the
   *                 other write models
   */
  type DeleteManyModel[TResult] = com.mongodb.client.model.DeleteManyModel[TResult]

  object DeleteManyModel {
    def apply(filter: Bson): DeleteManyModel[Nothing] = new com.mongodb.client.model.DeleteManyModel(filter)
  }

  /**
   * A model describing the removal of at most one document matching the query filter.
   *
   * @tparam TResult the type of document to update.  In practice this doesn't actually apply to updates but is here for consistency with
   *                 the other write models
   */
  type DeleteOneModel[TResult] = com.mongodb.client.model.DeleteOneModel[TResult]

  object DeleteOneModel {
    def apply(filter: Bson): DeleteOneModel[Nothing] = new com.mongodb.client.model.DeleteOneModel(filter)
  }

  /**
   * The options to apply to a find operation.
   */
  type FindOptions = com.mongodb.client.model.FindOptions

  object FindOptions {
    def apply(): FindOptions = new com.mongodb.client.model.FindOptions()

    /**
     * Construct a new instance by making a shallow copy of the given model.
     * @param from model to copy
     */
    def apply(from: FindOptions): FindOptions = new com.mongodb.client.model.FindOptions(from)
  }

  /**
   * The options to apply to an operation that atomically finds a document and deletes it.
   */
  type FindOneAndDeleteOptions = com.mongodb.client.model.FindOneAndDeleteOptions

  object FindOneAndDeleteOptions {
    def apply(): FindOneAndDeleteOptions = new com.mongodb.client.model.FindOneAndDeleteOptions()
  }

  /**
   * The options to apply to an operation that atomically finds a document and replaces it.
   */
  type FindOneAndReplaceOptions = com.mongodb.client.model.FindOneAndReplaceOptions

  object FindOneAndReplaceOptions {
    def apply(): FindOneAndReplaceOptions = new com.mongodb.client.model.FindOneAndReplaceOptions()
  }

  /**
   * The options to apply to an operation that atomically finds a document and updates it.
   */
  type FindOneAndUpdateOptions = com.mongodb.client.model.FindOneAndUpdateOptions

  object FindOneAndUpdateOptions {
    def apply(): FindOneAndUpdateOptions = new com.mongodb.client.model.FindOneAndUpdateOptions()
  }

  /**
   * The options to apply to an operation that inserts multiple documents into a collection.
   */
  type InsertManyOptions = com.mongodb.client.model.InsertManyOptions

  object InsertManyOptions {
    def apply(): InsertManyOptions = new com.mongodb.client.model.InsertManyOptions()
  }

  /**
   * The options to apply to the creation of an index.
   */
  type IndexOptions = com.mongodb.client.model.IndexOptions

  object IndexOptions {
    def apply(): IndexOptions = new com.mongodb.client.model.IndexOptions()
  }

  /**
   * A model describing the creation of a single index.
   */
  type IndexModel = com.mongodb.client.model.IndexModel

  object IndexModel {
    def apply(keys: Bson): IndexModel = new com.mongodb.client.model.IndexModel(keys)

    def apply(keys: Bson, indexOptions: IndexOptions): IndexModel = new com.mongodb.client.model.IndexModel(keys, indexOptions)
  }

  /**
   * A model describing an insert of a single document.
   *
   * @tparam TResult the type of document to insert. This can be of any type for which a `Codec` is registered
   */
  type InsertOneModel[TResult] = com.mongodb.client.model.InsertOneModel[TResult]

  object InsertOneModel {
    def apply[TResult](document: TResult): InsertOneModel[TResult] = new com.mongodb.client.model.InsertOneModel[TResult](document)
  }

  /**
   * The map reduce to collection actions.
   *
   * These actions are only available when passing out a collection that already exists. This option is not available on secondary members
   * of replica sets.  The Enum values dictate what to do with the output collection if it already exists when the map reduce is run.
   */
  type MapReduceAction = com.mongodb.client.model.MapReduceAction

  /**
   * The options to use for a parallel collection scan.
   */
  type ParallelCollectionScanOptions = com.mongodb.client.model.ParallelCollectionScanOptions

  object ParallelCollectionScanOptions {
    def apply(): ParallelCollectionScanOptions = new com.mongodb.client.model.ParallelCollectionScanOptions()
  }

  /**
   * The options to apply when renaming a collection
   */
  type RenameCollectionOptions = com.mongodb.client.model.RenameCollectionOptions

  object RenameCollectionOptions {
    def apply(): RenameCollectionOptions = new com.mongodb.client.model.RenameCollectionOptions()
  }

  /**
   * The options to apply to a `\$push` update operator.
   */
  type PushOptions = com.mongodb.client.model.PushOptions

  /**
   * Indicates which document to return, the original document before change or the document after the change
   */
  type ReturnDocument = com.mongodb.client.model.ReturnDocument

  /**
   * A model describing the replacement of at most one document that matches the query filter.
   *
   * @tparam TResult the type of document to replace. This can be of any type for which a `Codec` is registered
   */
  type ReplaceOneModel[TResult] = com.mongodb.client.model.ReplaceOneModel[TResult]

  object ReplaceOneModel {
    def apply[TResult](filter: Bson, replacement: TResult): ReplaceOneModel[TResult] =
      new com.mongodb.client.model.ReplaceOneModel[TResult](filter, replacement)

    def apply[TResult](filter: Bson, replacement: TResult, updateOptions: UpdateOptions): ReplaceOneModel[TResult] =
      new com.mongodb.client.model.ReplaceOneModel[TResult](filter, replacement, updateOptions)
  }

  /**
   * A model describing an update to all documents that matches the query filter. The update to apply must include only update
   * operators.
   *
   * @tparam TResult the type of document to update.  In practice this doesn't actually apply to updates but is here for consistency with the
   *                 other write models
   */
  type UpdateManyModel[TResult] = com.mongodb.client.model.UpdateManyModel[TResult]

  object UpdateManyModel {
    def apply(filter: Bson, update: Bson): UpdateManyModel[Nothing] = new com.mongodb.client.model.UpdateManyModel(filter, update)

    def apply(filter: Bson, update: Bson, updateOptions: UpdateOptions): UpdateManyModel[Nothing] =
      new com.mongodb.client.model.UpdateManyModel(filter, update, updateOptions)
  }

  /**
   * The options to apply when updating documents.
   */
  type UpdateOptions = com.mongodb.client.model.UpdateOptions

  object UpdateOptions {
    def apply(): UpdateOptions = new com.mongodb.client.model.UpdateOptions()
  }

  /**
   * A model describing an update to at most one document that matches the query filter. The update to apply must include only update
   * operators.
   *
   * @tparam TResult the type of document to update.  In practice this doesn't actually apply to updates but is here for consistency with the
   *                 other write models
   */
  type UpdateOneModel[TResult] = com.mongodb.client.model.UpdateOneModel[TResult]

  object UpdateOneModel {
    def apply(filter: Bson, update: Bson): UpdateOneModel[Nothing] = new com.mongodb.client.model.UpdateOneModel(filter, update)

    def apply(filter: Bson, update: Bson, updateOptions: UpdateOptions): UpdateOneModel[Nothing] =
      new com.mongodb.client.model.UpdateOneModel(filter, update, updateOptions)
  }

  /**
   * A base class for models that can be used in a bulk write operations.
   *
   * @tparam TResult the document type for storage
   */
  type WriteModel[TResult] = com.mongodb.client.model.WriteModel[TResult]

}
