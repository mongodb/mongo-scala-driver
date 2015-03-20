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

import java.util

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model._
import com.mongodb.client.result.{ DeleteResult, UpdateResult }
import com.mongodb.reactivestreams.client.{ MongoCollection => JMongoCollection, Success }
import com.mongodb.{ MongoNamespace, ReadPreference, WriteConcern }
import com.mongodb.scala.reactivestreams.client.collection.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.reactivestreams.Publisher

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import com.mongodb.scala.reactivestreams.client.Helpers.{ DefaultsTo, classTagToClassOf }

// scalastyle:off number.of.methods

/**
 * The MongoCollection representation.
 *
 * @param wrapped the underlying java MongoCollection
 * @tparam T The type that this collection will encode documents from and decode documents to.
 */
case class MongoCollection[T](private val wrapped: JMongoCollection[T]) {
  /**
   * Gets the namespace of this collection.
   *
   * @return the namespace
   */
  lazy val namespace: MongoNamespace = wrapped.getNamespace

  /**
   * Get the default class to cast any documents returned from the database into.
   *
   * @return the default class to cast any documents into
   */
  lazy val documentClass: Class[T] = wrapped.getDocumentClass

  /**
   * Get the codec registry for the MongoDatabase.
   *
   * @return the { @link org.bson.codecs.configuration.CodecRegistry}
   */
  lazy val codecRegistry: CodecRegistry = wrapped.getCodecRegistry

  /**
   * Get the read preference for the MongoDatabase.
   *
   * @return the { @link com.mongodb.ReadPreference}
   */
  lazy val readPreference: ReadPreference = wrapped.getReadPreference

  /**
   * Get the write concern for the MongoDatabase.
   *
   * @return the { @link com.mongodb.WriteConcern}
   */
  lazy val writeConcern: WriteConcern = wrapped.getWriteConcern

  /**
   * Create a new MongoCollection instance with a different default class to cast any documents returned from the database into..
   *
   * @tparam C   The type that the new collection will encode documents from and decode documents to
   * @return a new MongoCollection instance with the different default class
   */
  def withDocumentClass[C]()(implicit e: C DefaultsTo Document, ct: ClassTag[C]): MongoCollection[C] =
    MongoCollection(wrapped.withDocumentClass(ct))

  /**
   * Create a new MongoCollection instance with a different codec registry.
   *
   * @param codecRegistry the new { @link org.bson.codecs.configuration.CodecRegistry} for the collection
   * @return a new MongoCollection instance with the different codec registry
   */
  def withCodecRegistry(codecRegistry: CodecRegistry): MongoCollection[T] = MongoCollection(wrapped.withCodecRegistry(codecRegistry))

  /**
   * Create a new MongoCollection instance with a different read preference.
   *
   * @param readPreference the new { @link com.mongodb.ReadPreference} for the collection
   * @return a new MongoCollection instance with the different readPreference
   */
  def withReadPreference(readPreference: ReadPreference): MongoCollection[T] = MongoCollection(wrapped.withReadPreference(readPreference))

  /**
   * Create a new MongoCollection instance with a different write concern.
   *
   * @param writeConcern the new { @link com.mongodb.WriteConcern} for the collection
   * @return a new MongoCollection instance with the different writeConcern
   */
  def withWriteConcern(writeConcern: WriteConcern): MongoCollection[T] = MongoCollection(wrapped.withWriteConcern(writeConcern))

  /**
   * Counts the number of documents in the collection.
   *
   * @return a publisher with a single element indicating the number of documents
   */
  def count(): Publisher[Long] = wrapped.count().asInstanceOf[Publisher[Long]]

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter the query filter
   * @return a publisher with a single element indicating the number of documents
   */
  def count(filter: Bson): Publisher[Long] = wrapped.count(filter).asInstanceOf[Publisher[Long]]

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter  the query filter
   * @param options the options describing the count
   * @return a publisher with a single element indicating the number of documents
   */
  def count(filter: Bson, options: CountOptions): Publisher[Long] = wrapped.count(filter, options).asInstanceOf[Publisher[Long]]

  /**
   * Gets the distinct values of the specified field name.
   *
   * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
   * @param fieldName the field name
   * @tparam C       the target type of the iterable.
   * @return a publisher emitting the sequence of distinct values
   */
  def distinct[C](fieldName: String)(implicit ct: ClassTag[C]): DistinctPublisher[C] =
    DistinctPublisher(wrapped.distinct(fieldName, ct))

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   *
   * @tparam C   the target document type of the iterable.
   * @return the find publisher
   */
  def find[C]()(implicit e: C DefaultsTo Document, ct: ClassTag[C]): FindPublisher[C] =
    FindPublisher(wrapped.find[C](ct))

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   * @param filter the query filter
   * @tparam C    the target document type of the iterable.
   * @return the find publisher
   */
  def find[C](filter: Bson)(implicit e: C DefaultsTo Document, ct: ClassTag[C]): FindPublisher[C] =
    FindPublisher(wrapped.find(filter, ct))

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @return a publisher containing the result of the aggregation operation
   *         [[http://docs.mongodb.org/manual/aggregation/ Aggregation]]
   */
  def aggregate[C](pipeline: List[Bson])(implicit e: C DefaultsTo Document, ct: ClassTag[C]): AggregatePublisher[C] =
    AggregatePublisher(wrapped.aggregate[C](pipeline.asJava, ct))

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @param clazz          the class to decode each resulting document into.
   * @tparam C            the target document type of the iterable.
   * @return a publisher containing the result of the map-reduce operation
   *         [[http://docs.mongodb.org/manual/reference/command/mapReduce/ map-reduce]]
   */
  def mapReduce[C](mapFunction: String, reduceFunction: String)(implicit e: C DefaultsTo Document, ct: ClassTag[C]): MapReducePublisher[C] =
    MapReducePublisher(wrapped.mapReduce(mapFunction, reduceFunction, ct))

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   * @return a publisher with a single element the BulkWriteResult
   */
  def bulkWrite(requests: List[_ <: WriteModel[_ <: T]]): Publisher[BulkWriteResult] =
    wrapped.bulkWrite(requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: T]]])

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   * @param options  the options to apply to the bulk write operation
   * @return a publisher with a single element the BulkWriteResult
   */
  def bulkWrite(requests: List[_ <: WriteModel[_ <: T]], options: BulkWriteOptions): Publisher[BulkWriteResult] =
    wrapped.bulkWrite(requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: T]]], options)

  /**
   * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
   *
   * @param document the document to insert
   * @return a publisher with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertOne(document: T): Publisher[Success] = wrapped.insertOne(document)

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   * @return a publisher with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertMany(documents: List[_ <: T]): Publisher[Success] = wrapped.insertMany(documents.asJava)

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   * @param options   the options to apply to the operation
   * @return a publisher with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertMany(documents: List[_ <: T], options: InsertManyOptions): Publisher[Success] = wrapped.insertMany(documents.asJava, options)

  /**
   * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
   * modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @return a publisher with a single element the DeleteResult or with an com.mongodb.MongoException
   */
  def deleteOne(filter: Bson): Publisher[DeleteResult] = wrapped.deleteOne(filter)

  /**
   * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @return a publisher with a single element the DeleteResult or with an com.mongodb.MongoException
   */
  def deleteMany(filter: Bson): Publisher[DeleteResult] = wrapped.deleteMany(filter)

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a publisher with a single element the UpdateResult
   */
  def replaceOne(filter: Bson, replacement: T): Publisher[UpdateResult] = wrapped.replaceOne(filter, replacement)

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the replace operation
   * @return a publisher with a single element the UpdateResult
   */
  def replaceOne(filter: Bson, replacement: T, options: UpdateOptions): Publisher[UpdateResult] =
    wrapped.replaceOne(filter, replacement, options)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
   * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
   * @param filter a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *               registered
   * @param update a document describing the update, which may not be null. The update to apply must include only update operators. This
   *               can be of any type for which a { @code Codec} is registered
   * @return a publisher with a single element the UpdateResult
   */
  def updateOne(filter: Bson, update: Bson): Publisher[UpdateResult] = wrapped.updateOne(filter, update)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
   * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
   * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                registered
   * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                can be of any type for which a { @code Codec} is registered
   * @param options the options to apply to the update operation
   * @return a publisher with a single element the UpdateResult
   */
  def updateOne(filter: Bson, update: Bson, options: UpdateOptions): Publisher[UpdateResult] =
    wrapped.updateOne(filter, update, options)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
   * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
   * @param filter a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *               registered
   * @param update a document describing the update, which may not be null. The update to apply must include only update operators. This
   *               can be of any type for which a { @code Codec} is registered
   * @return a publisher with a single element the UpdateResult
   */
  def updateMany(filter: Bson, update: Bson): Publisher[UpdateResult] = wrapped.updateMany(filter, update)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
   * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
   * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                registered
   * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                can be of any type for which a { @code Codec} is registered
   * @param options the options to apply to the update operation
   * @return a publisher with a single element the UpdateResult
   */
  def updateMany(filter: Bson, update: Bson, options: UpdateOptions): Publisher[UpdateResult] =
    wrapped.updateMany(filter, update, options)

  /**
   * Atomically find a document and remove it.
   *
   * @param filter the query filter to find the document with
   * @return a publisher with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   */
  def findOneAndDelete(filter: Bson): Publisher[T] = wrapped.findOneAndDelete(filter)

  /**
   * Atomically find a document and remove it.
   *
   * @param filter  the query filter to find the document with
   * @param options the options to apply to the operation
   * @return a publisher with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   */
  def findOneAndDelete(filter: Bson, options: FindOneAndDeleteOptions): Publisher[T] = wrapped.findOneAndDelete(filter, options)

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a publisher with a single element the document that was replaced.  Depending on the value of the { @code returnOriginal}
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndReplace(filter: Bson, replacement: T): Publisher[T] = wrapped.findOneAndReplace(filter, replacement)

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the operation
   * @return a publisher with a single element the document that was replaced.  Depending on the value of the { @code returnOriginal}
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndReplace(filter: Bson, replacement: T, options: FindOneAndReplaceOptions): Publisher[T] =
    wrapped.findOneAndReplace(filter, replacement, options)

  /**
   * Atomically find a document and update it.
   *
   * @param filter a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *               registered
   * @param update a document describing the update, which may not be null. The update to apply must include only update operators. This
   *               can be of any type for which a { @code Codec} is registered
   * @return a publisher with a single element the document that was updated before the update was applied.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndUpdate(filter: Bson, update: Bson): Publisher[T] = wrapped.findOneAndUpdate(filter, update)

  /**
   * Atomically find a document and update it.
   *
   * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                registered
   * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                can be of any type for which a { @code Codec} is registered
   * @param options the options to apply to the operation
   * @return a publisher with a single element the document that was updated.  Depending on the value of the { @code returnOriginal}
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndUpdate(filter: Bson, update: Bson, options: FindOneAndUpdateOptions): Publisher[T] =
    wrapped.findOneAndUpdate(filter, update, options)

  /**
   * Drops this collection from the Database.
   *
   * @return a publisher with a single element indicating when the operation has completed
   *         [[http://docs.mongodb.org/manual/reference/command/drop/ Drop Collection]]
   */
  def drop(): Publisher[Success] = wrapped.drop()

  /**
   * Create an Index
   *
   * @param key an object describing the index key(s), which may not be null. This can be of any type for which a { @code Codec} is
   *            registered
   * @return a publisher with a single element indicating when the operation has completed
   *         [[http://docs.mongodb.org/manual/reference/method/db.collection.ensureIndex Ensure Index]]
   */
  def createIndex(key: Bson): Publisher[Success] = wrapped.createIndex(key)

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a { @code Codec} is
   *                registered
   * @param options the options for the index
   * @return a publisher with a single element indicating when the operation has completed
   */
  def createIndex(key: Bson, options: IndexOptions): Publisher[Success] = wrapped.createIndex(key, options)

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param indexes the list of indexes to create
   * @return a publisher with a single element indicating when the operation has completed
   */
  def createIndexes(models: List[IndexModel]): Publisher[Success] = wrapped.createIndexes(models.asJava)

  /**
   * Get all the indexes in this collection.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listIndexes/ listIndexes]]
   * @tparam C   the target document type of the iterable.
   * @return the fluent list indexes interface
   */
  def listIndexes[C]()(implicit e: C DefaultsTo Document, ct: ClassTag[C]): ListIndexesPublisher[C] =
    ListIndexesPublisher(wrapped.listIndexes(ct))

  /**
   * Drops the given index.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param indexName the name of the index to remove
   * @return a publisher with a single element indicating when the operation has completed
   */
  def dropIndex(indexName: String): Publisher[Success] = wrapped.dropIndex(indexName)

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @return a publisher with a single element indicating when the operation has completed
   */
  def dropIndexes(): Publisher[Success] = wrapped.dropIndexes()

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param newCollectionNamespace the namespace the collection will be renamed to
   * @return a publisher with a single element indicating when the operation has completed
   */
  def renameCollection(newCollectionNamespace: MongoNamespace): Publisher[Success] = wrapped.renameCollection(newCollectionNamespace)

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param newCollectionNamespace the name the collection will be renamed to
   * @param options                the options for renaming a collection
   * @return a publisher with a single element indicating when the operation has completed
   */
  def renameCollection(newCollectionNamespace: MongoNamespace, options: RenameCollectionOptions): Publisher[Success] =
    wrapped.renameCollection(newCollectionNamespace, options)

}
// scalastyle:on number.of.methods
