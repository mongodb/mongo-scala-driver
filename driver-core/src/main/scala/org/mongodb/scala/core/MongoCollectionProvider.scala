/**
 * Copyright (c) 2014 MongoDB, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * For questions and comments about this product, please see the project page at:
 *
 * https://github.com/mongodb/mongo-scala-driver
 */
package org.mongodb.scala.core

import java.lang.Long
import java.lang.String._
import java.util
import java.util.Arrays._
import java.util.concurrent.TimeUnit._

import com.mongodb.ReadPreference._
import com.mongodb.bulk.{ BulkWriteResult, DeleteRequest, InsertRequest, UpdateRequest, WriteRequest }
import com.mongodb.client.model._
import com.mongodb.client.options.OperationOptions
import com.mongodb.client.result.{ DeleteResult, UpdateResult }
import com.mongodb.operation._
import com.mongodb.{ MongoNamespace, WriteConcernResult }
import org.bson.codecs.{ Codec, CollectibleCodec }
import org.bson.{ BsonArray, BsonDocument, BsonDocumentWrapper, BsonJavaScript, BsonString, Document }

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/**
 * The MongoCollectionProvider trait providing the core of a MongoCollection implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the
 * concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation. To do this the concrete implementation of this trait requires the following
 * methods to be implemented:
 *
 * {{{
 *    case class MongoCollection[T](name: String, database: MongoDatabase, codec: CollectibleCodec[T],
 *                                  options: MongoCollectionOptions) extends MongoCollectionProvider[T] with RequiredTypesAndTransformers {
 *
 *      val admin: MongoCollectionAdmin[T] = MongoCollectionAdmin(this)
 *
 *      protected def collectionView: MongoCollectionViewProvider[T]
 *
 *    }
 * }}}
 *
 */
trait MongoCollectionProvider[T] extends ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * The namespace of the collection
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val namespace: MongoNamespace

  /**
   * The OperationOptions to be used with this MongoCollection instance
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val options: OperationOptions

  val executor: AsyncOperationExecutor

  val clazz: Class[T]

  /**
   * Counts the number of documents in the collection.
   */
  def count(): ResultType[Long] = count(new Document(), new CountOptions())

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter   the query filter
   */
  def count(filter: Any): ResultType[Long] = count(filter, new CountOptions())

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter   the query filter
   * @param options  the options describing the count
   */
  def count(filter: Any, options: CountOptions): ResultType[Long] = {
    val operation: CountOperation = new CountOperation(namespace).filter(asBson(filter)).skip(options.getSkip)
      .limit(options.getLimit).maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
    if (options.getHint != null) {
      operation.hint(asBson(options.getHint))
    } else if (options.getHintString != null) {
      operation.hint(new BsonString(options.getHintString))
    }
    executeAsync(operation, this.options.getReadPreference, resultTypeCallback[Long]())
  }

  /**
   * Gets the distinct values of the specified field name.
   *
   * @param fieldName the field name
   * @param filter    the query filter
   */
  def distinct(fieldName: String, filter: Any): ResultType[BsonArray] =
    distinct(fieldName, new DistinctOptions())

  /**
   * Gets the distinct values of the specified field name.
   *
   * TODO - remove / transform bson array to at least a list of items
   *
   * @param fieldName the field name
   * @param filter    the query filter
   * @param options   the options to apply to the distinct operation
   */
  def distinct(fieldName: String, filter: Any, options: DistinctOptions): ResultType[BsonArray] = {
    val operation: DistinctOperation = new DistinctOperation(namespace, fieldName).filter(asBson(filter))
      .maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
    executeAsync(operation, this.options.getReadPreference, resultTypeCallback[BsonArray]())
  }

  /**
   * Finds all documents in the collection.
   *
   * @return the fluent find interface
   */
  def find(): FindFluent[T] = findFluent[T](namespace, new BsonDocument(), new FindOptions(), options, executor, clazz)

  /**
   * Finds all documents in the collection.
   *
   * @tparam C   the target document type of the iterable.
   * @return the fluent find interface
   */
  def find[C](clazz: Class[C]): FindFluent[C] = findFluent[C](namespace, new BsonDocument(), new FindOptions(),
    options, executor, clazz)

  /**
   * Finds all documents in the collection.
   *
   * @param filter the query filter
   * @return the fluent find interface
   */
  def find(filter: Any): FindFluent[T] = find().filter(filter).asInstanceOf[FindFluent[T]]

  /**
   * Finds all documents in the collection.
   *
   * @param filter the query filter
   * @return the fluent find interface
   */
  def find[C](filter: Any, clazz: Class[C]): FindFluent[C] = find[C](clazz).filter(filter).asInstanceOf[FindFluent[C]]

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @return an iterable containing the result of the aggregation operation
   */
  def aggregate(pipeline: Iterable[_]): MongoIterable[Document] =
    aggregate[Document](pipeline, new AggregateOptions(), classOf[Document])

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @tparam C      the target document type of the iterable.
   * @return an iterable containing the result of the aggregation operation
   */
  def aggregate[C](pipeline: Iterable[_], clazz: Class[C]): MongoIterable[C] =
    aggregate[C](pipeline, new AggregateOptions(), clazz)

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @param options  the options to apply to the aggregation operation
   * @return an iterable containing the result of the aggregation operation
   */
  def aggregate(pipeline: Iterable[_], options: AggregateOptions): MongoIterable[Document] =
    aggregate[Document](pipeline, options, classOf[Document])

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @param options  the options to apply to the aggregation operation
   * @tparam C      the target document type of the iterable.
   * @return an iterable containing the result of the aggregation operation
   */
  def aggregate[C](pipeline: Iterable[_], options: AggregateOptions, clazz: Class[C]): MongoIterable[C] = {
    val aggregateList: util.List[BsonDocument] = createBsonDocumentList(pipeline)
    val outCollection: Option[String] = aggregateList.size == 0 match {
      case true  => None
      case false => Some(aggregateList.get(aggregateList.size - 1).getString("$out").getValue)
    }

    outCollection match {
      case Some(collectionName) =>
        val operation: AggregateToCollectionOperation = new AggregateToCollectionOperation(namespace, aggregateList)
          .maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
          .allowDiskUse(options.getAllowDiskUse)
        // Todo race condition.
        executeAsync(operation, voidToUnitResultTypeCallback())
        findFluent[C](new MongoNamespace(namespace.getDatabaseName, collectionName), new BsonDocument(),
          new FindOptions(), this.options, executor, clazz)
      case None => operationIterable[C](new AggregateOperation[C](namespace, aggregateList, getCodec(clazz))
        .maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
        .allowDiskUse(options.getAllowDiskUse).batchSize(options.getBatchSize)
        .useCursor(options.getUseCursor), this.options.getReadPreference, executor,
        clazz)
    }
  }

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @return an iterable containing the result of the map-reduce operation
   */
  def mapReduce(mapFunction: String, reduceFunction: String): MongoIterable[Document] =
    mapReduce[Document](mapFunction, reduceFunction, new MapReduceOptions(), classOf[Document])

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @param options        The specific options for the map-reduce command.
   * @return an iterable containing the result of the map-reduce operation
   */
  def mapReduce(mapFunction: String, reduceFunction: String, options: MapReduceOptions): MongoIterable[Document] =
    mapReduce[Document](mapFunction, reduceFunction, options, classOf[Document])

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @tparam C            the target document type of the iterable.
   * @return an iterable containing the result of the map-reduce operation
   */
  def mapReduce[C](mapFunction: String, reduceFunction: String, clazz: Class[C]): MongoIterable[C] =
    mapReduce[C](mapFunction, reduceFunction, new MapReduceOptions(), clazz)

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @param options        The specific options for the map-reduce command.
   * @tparam C            the target document type of the iterable.
   * @return an iterable containing the result of the map-reduce operation
   */
  def mapReduce[C](mapFunction: String, reduceFunction: String, options: MapReduceOptions,
                   clazz: Class[C]): MongoIterable[C] = {
    options.isInline match {
      case true =>
        val operation: MapReduceWithInlineResultsOperation[C] =
          new MapReduceWithInlineResultsOperation[C](namespace, new BsonJavaScript(mapFunction),
            new BsonJavaScript(reduceFunction), getCodec(clazz))
            .filter(asBson(options.getFilter)).limit(options.getLimit)
            .maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
            .jsMode(options.isJsMode).scope(asBson(options.getScope))
            .sort(asBson(options.getSort)).verbose(options.isVerbose)
        Option(options.getFinalizeFunction) match {
          case Some(finalize) => operation.finalizeFunction(new BsonJavaScript(finalize))
          case None           =>
        }
        operationIterable[C](operation.asInstanceOf[AsyncReadOperation[AsyncBatchCursor[C]]],
          this.options.getReadPreference, executor, clazz)
      case false =>
        val operation: MapReduceToCollectionOperation =
          new MapReduceToCollectionOperation(namespace, new BsonJavaScript(mapFunction),
            new BsonJavaScript(reduceFunction), options.getCollectionName)
            .filter(asBson(options.getFilter)).limit(options.getLimit)
            .maxTime(options.getMaxTime(MILLISECONDS), MILLISECONDS)
            .jsMode(options.isJsMode).scope(asBson(options.getScope))
            .sort(asBson(options.getSort)).verbose(options.isVerbose)
            .action(options.getAction.getValue).nonAtomic(options.isNonAtomic)
            .sharded(options.isSharded).databaseName(options.getDatabaseName)
        Option(options.getFinalizeFunction) match {
          case Some(finalize) => operation.finalizeFunction(new BsonJavaScript(finalize))
          case None           =>
        }
        // Todo race condition.
        executeAsync(operation, ignoreResultTypeCallback[MapReduceStatistics]())

        val databaseName: String = Option(options.getDatabaseName) match {
          case Some(dbName) => dbName
          case None         => namespace.getDatabaseName
        }
        val readOptions: OperationOptions = OperationOptions.builder.readPreference(primary).build
          .withDefaults(this.options)
        findFluent[C](new MongoNamespace(databaseName, options.getCollectionName), new BsonDocument(),
          new FindOptions(), this.options, executor, clazz)
    }
  }

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   */
  def bulkWrite(requests: Iterable[_ <: WriteModel[_ <: T]]): ResultType[BulkWriteResult] =
    bulkWrite(requests, new BulkWriteOptions)

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   * @param options  the options to apply to the bulk write operation
   */
  def bulkWrite(requests: Iterable[_ <: WriteModel[_ <: T]], options: BulkWriteOptions): ResultType[BulkWriteResult] = {
    val writeRequests: mutable.Buffer[WriteRequest] = mutable.Buffer[WriteRequest]()
    for (writeModel <- requests) {
      val writeRequest: WriteRequest = writeModel match {
        case _: InsertOneModel[_] =>
          val insertOneModel: InsertOneModel[T] = writeModel.asInstanceOf[InsertOneModel[T]]
          if (getCodec.isInstanceOf[CollectibleCodec[_]]) {
            getCodec.asInstanceOf[CollectibleCodec[T]].generateIdIfAbsentFromDocument(insertOneModel.getDocument)
          }
          new InsertRequest(asBson(insertOneModel.getDocument))
        case _: ReplaceOneModel[_] =>
          val replaceOneModel: ReplaceOneModel[T] = writeModel.asInstanceOf[ReplaceOneModel[T]]
          new UpdateRequest(asBson(replaceOneModel.getFilter), asBson(replaceOneModel.getReplacement),
            WriteRequest.Type.REPLACE).upsert(replaceOneModel.getOptions.isUpsert)
        case _: UpdateOneModel[_] =>
          val updateOneModel: UpdateOneModel[T] = writeModel.asInstanceOf[UpdateOneModel[T]]
          new UpdateRequest(asBson(updateOneModel.getFilter), asBson(updateOneModel.getUpdate),
            WriteRequest.Type.UPDATE).multi(false)
            .upsert(updateOneModel.getOptions.isUpsert)
        case _: UpdateManyModel[_] =>
          val updateManyModel: UpdateManyModel[T] = writeModel.asInstanceOf[UpdateManyModel[T]]
          new UpdateRequest(asBson(updateManyModel.getFilter), asBson(updateManyModel.getUpdate),
            WriteRequest.Type.UPDATE).multi(true)
            .upsert(updateManyModel.getOptions.isUpsert)
        case _: DeleteOneModel[_] =>
          val deleteOneModel: DeleteOneModel[T] = writeModel.asInstanceOf[DeleteOneModel[T]]
          new DeleteRequest(asBson(deleteOneModel.getFilter)).multi(false)
        case _: DeleteManyModel[_] =>
          val deleteManyModel: DeleteManyModel[T] = writeModel.asInstanceOf[DeleteManyModel[T]]
          new DeleteRequest(asBson(deleteManyModel.getFilter)).multi(true)
        case _ =>
          throw new UnsupportedOperationException(format("WriteModel of type %s is not supported", writeModel.getClass))
      }
      writeRequests += writeRequest
    }
    executeAsync(new MixedBulkWriteOperation(namespace, writeRequests.toList.asJava, options.isOrdered,
      this.options.getWriteConcern),
      resultTypeCallback[BulkWriteResult]())
  }

  /**
   * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
   *
   * @param document the document to insert
   */
  def insertOne(document: T): ResultType[WriteConcernResult] = {
    if (getCodec.isInstanceOf[CollectibleCodec[_]]) {
      getCodec.asInstanceOf[CollectibleCodec[T]].generateIdIfAbsentFromDocument(document)
    }
    executeAsync(new InsertOperation(namespace, true, options.getWriteConcern,
      List(new InsertRequest(asBson(document))).asJava),
      resultTypeCallback[WriteConcernResult]())
  }

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   */
  def insertMany(documents: Iterable[_ <: T]): ResultType[WriteConcernResult] =
    insertMany(documents, new InsertManyOptions)

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   * @param options   the options to apply to the operation
   */
  def insertMany(documents: Iterable[_ <: T], options: InsertManyOptions): ResultType[WriteConcernResult] = {
    val requests: mutable.Buffer[InsertRequest] = mutable.Buffer[InsertRequest]()
    for (document <- documents) {
      if (getCodec.isInstanceOf[CollectibleCodec[_]]) {
        getCodec.asInstanceOf[CollectibleCodec[T]].generateIdIfAbsentFromDocument(document)
      }
      requests += new InsertRequest(asBson(document))
    }
    executeAsync(new InsertOperation(namespace, options.isOrdered, this.options.getWriteConcern,
      requests.toList.asJava), resultTypeCallback[WriteConcernResult]())
  }

  /**
   * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
   * modified.
   *
   * @param filter   the query filter to apply the the delete operation
   */
  def deleteOne(filter: Any): ResultType[DeleteResult] = executeDelete(filter, multi = false)

  /**
   * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
   *
   * @param filter   the query filter to apply the the delete operation
   */
  def deleteMany(filter: Any): ResultType[DeleteResult] = executeDelete(filter, multi = true)

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   */
  def replaceOne(filter: Any, replacement: T): ResultType[UpdateResult] =
    replaceOne(filter, replacement, new UpdateOptions)

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the replace operation
   */
  def replaceOne(filter: Any, replacement: T, options: UpdateOptions): ResultType[UpdateResult] = {
    val requests: List[UpdateRequest] = List(new UpdateRequest(asBson(filter), asBson(replacement),
      WriteRequest.Type.REPLACE).upsert(options.isUpsert))
    // TODO modified count
    executeAsync(new UpdateOperation(namespace, true, this.options.getWriteConcern, requests.asJava),
      updateResultCallback())
  }

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   */
  def updateOne(filter: Any, update: Any): ResultType[UpdateResult] =
    updateOne(filter, update, new UpdateOptions)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   * @param options  the options to apply to the update operation
   */
  def updateOne(filter: Any, update: Any, options: UpdateOptions): ResultType[UpdateResult] =
    executeUpdate(filter, update, options, multi = false)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   */
  def updateMany(filter: Any, update: Any): ResultType[UpdateResult] =
    updateMany(filter, update, new UpdateOptions)

  /**
   * Update a single document in the collection according to the specified arguments.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   * @param options  the options to apply to the update operation
   */
  def updateMany(filter: Any, update: Any, options: UpdateOptions): ResultType[UpdateResult] =
    executeUpdate(filter, update, options, multi = true)

  /**
   * Atomically find a document and remove it.
   *
   * @param filter   the query filter to find the document with
   */
  def findOneAndDelete(filter: Any): ResultType[T] =
    findOneAndDelete(filter, new FindOneAndDeleteOptions)

  /**
   * Atomically find a document and remove it.
   *
   * @param filter   the query filter to find the document with
   * @param options  the options to apply to the operation
   */
  def findOneAndDelete(filter: Any, options: FindOneAndDeleteOptions): ResultType[T] =
    executeAsync(new FindAndDeleteOperation[T](namespace, getCodec).filter(asBson(filter))
      .projection(asBson(options.getProjection)).sort(asBson(options.getSort)), resultTypeCallback[T]())

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   */
  def findOneAndReplace(filter: Any, replacement: T): ResultType[T] =
    findOneAndReplace(filter, replacement, new FindOneAndReplaceOptions)

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the operation
   */
  def findOneAndReplace(filter: Any, replacement: T, options: FindOneAndReplaceOptions): ResultType[T] =
    executeAsync(new FindAndReplaceOperation[T](namespace, getCodec, asBson(replacement)).filter(asBson(filter))
      .projection(asBson(options.getProjection)).sort(asBson(options.getSort))
      .returnOriginal(options.getReturnOriginal).upsert(options.isUpsert), resultTypeCallback[T]())

  /**
   * Atomically find a document and update it.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   */
  def findOneAndUpdate(filter: Any, update: Any): ResultType[T] =
    findOneAndUpdate(filter, update, new FindOneAndUpdateOptions)

  /**
   * Atomically find a document and update it.
   *
   * @param filter   a document describing the query filter, which may not be null. This can be of any type for which a { @code Codec} is
   *                 registered
   * @param update   a document describing the update, which may not be null. The update to apply must include only update operators. This
   *                 can be of any type for which a { @code Codec} is registered
   * @param options  the options to apply to the operation
   */
  def findOneAndUpdate(filter: Any, update: Any, options: FindOneAndUpdateOptions): ResultType[T] =
    executeAsync(new FindAndUpdateOperation[T](namespace, getCodec, asBson(update)).filter(asBson(filter))
      .projection(asBson(options.getProjection)).sort(asBson(options.getSort))
      .returnOriginal(options.getReturnOriginal).upsert(options.isUpsert), resultTypeCallback[T]())

  /**
   * Drops this collection from the Database.
   *
   */
  def dropCollection(): ResultType[Unit] =
    executeAsync(new DropCollectionOperation(namespace), voidToUnitResultTypeCallback())

  /**
   * Creates an Index on the collection
   *
   * @param key an object describing the index key(s), which may not be null. This can be of any type for which a { @code Codec} is
   *            registered
   */
  def createIndex(key: Any): ResultType[Unit] =
    createIndex(key, new CreateIndexOptions)

  /**
   * Creates an Index on the collection
   *
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a { @code Codec} is
   *                registered
   * @param options the options for the index
   */
  def createIndex(key: Any, options: CreateIndexOptions): ResultType[Unit] =
    executeAsync(new CreateIndexOperation(namespace, asBson(key))
      .name(options.getName).background(options.isBackground).unique(options.isUnique).sparse(options.isSparse)
      .expireAfterSeconds(options.getExpireAfterSeconds).version(options.getVersion)
      .weights(asBson(options.getWeights)).defaultLanguage(options.getDefaultLanguage)
      .languageOverride(options.getLanguageOverride).textIndexVersion(options.getTextIndexVersion)
      .twoDSphereIndexVersion(options.getTwoDSphereIndexVersion).bits(options.getBits).min(options.getMin)
      .max(options.getMax).bucketSize(options.getBucketSize), voidToUnitResultTypeCallback())

  /**
   * Gets all the indexes for the collection
   */
  def indexes(): ListResultType[T] =
    indexes[T](clazz)

  /**
   * Gets all the indexes for the collection
   *
   * @tparam C      the target document type of the iterable.
   */
  def indexes[C](clazz: Class[C]): ListResultType[C] =
    executeAsync(new ListIndexesOperation(namespace, getCodec(clazz)), options.getReadPreference,
      listToListResultTypeCallback[C]())

  /**
   * Drops the given index.
   *
   * @param indexName the name of the index to remove
   */
  def dropIndex(indexName: String): ResultType[Unit] =
    executeAsync(new DropIndexOperation(namespace, indexName), voidToUnitResultTypeCallback())

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   */
  def dropIndexes(): ResultType[Unit] = dropIndex("*")

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * @param newCollectionNamespace the namespace the collection will be renamed to
   * @throws com.mongodb.MongoServerException if you provide a newCollectionName that is the name of an existing collection, or if the
   *                                          oldCollectionName is the name of a collection that doesn't exist
   */
  def renameCollection(newCollectionNamespace: MongoNamespace): ResultType[Unit] =
    renameCollection(newCollectionNamespace, new RenameCollectionOptions)

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * @param newCollectionNamespace the name the collection will be renamed to
   * @param options                the options for renaming a collection
   * @throws com.mongodb.MongoServerException if you provide a newCollectionName that is the name of an existing collection and dropTarget
   *                                          is false, or if the oldCollectionName is the name of a collection that doesn't exist
   */
  def renameCollection(newCollectionNamespace: MongoNamespace, options: RenameCollectionOptions): ResultType[Unit] =
    executeAsync(new RenameCollectionOperation(namespace, newCollectionNamespace).dropTarget(options.isDropTarget),
      voidToUnitResultTypeCallback())

  private def executeDelete(filter: Any, multi: Boolean): ResultType[DeleteResult] = {
    executeAsync(new DeleteOperation(namespace, true, options.getWriteConcern,
      asList(new DeleteRequest(asBson(filter)).multi(multi))),
      new ResultCallback[WriteConcernResult, DeleteResult, ResultType[DeleteResult]] {
        val map: (WriteConcernResult) => DeleteResult = (result: WriteConcernResult) =>
          result.wasAcknowledged match {
            case true  => DeleteResult.acknowledged(result.getCount)
            case false => DeleteResult.unacknowledged
          }
        val transformer: (Future[DeleteResult]) => ResultType[DeleteResult] =
          resultTypeConverter[DeleteResult]()
      })
  }

  private def executeUpdate(filter: Any, update: Any, updateOptions: UpdateOptions,
                            multi: Boolean): ResultType[UpdateResult] = {
    executeAsync(new UpdateOperation(namespace, true, options.getWriteConcern,
      asList(new UpdateRequest(asBson(filter), asBson(update),
        WriteRequest.Type.UPDATE)
        .upsert(updateOptions.isUpsert)
        .multi(multi))),
      updateResultCallback())
  }

  private def updateResultCallback() = new ResultCallback[WriteConcernResult, UpdateResult, ResultType[UpdateResult]] {
    val map: (WriteConcernResult) => UpdateResult = (result: WriteConcernResult) =>
      result.wasAcknowledged match {
        case true  => UpdateResult.acknowledged(result.getCount, 0, result.getUpsertedId)
        case false => UpdateResult.unacknowledged
      }
    val transformer: (Future[UpdateResult]) => ResultType[UpdateResult] = resultTypeConverter[UpdateResult]()
  }

  private def getCodec: Codec[T] = getCodec(clazz)

  private def getCodec[C](clazz: Class[C]): Codec[C] = options.getCodecRegistry.get(clazz)

  private def asBson(document: Any): BsonDocument =
    BsonDocumentWrapper.asBsonDocument(document, options.getCodecRegistry)

  private def createBsonDocumentList[D](pipeline: Iterable[D]): util.List[BsonDocument] = {
    val aggregateList: ListBuffer[BsonDocument] = new ListBuffer[BsonDocument]()
    for (obj <- pipeline) {
      aggregateList.append(asBson(obj))
    }
    aggregateList.toList.asJava
  }
}
