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
 * [Project URL - TODO]
 *
 */
package org.mongodb.scala

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import rx.lang.scala.{Observable, Subject}

import org.mongodb.{CollectibleCodec, ConvertibleToDocument, Document, Get, MongoNamespace, QueryOptions, ReadPreference, WriteConcern, WriteResult}
import org.mongodb.operation._

import org.mongodb.scala.AsyncImplicitHelpers._
import org.mongodb.scala.admin.MongoCollectionAdmin
import org.mongodb.scala.utils.HandleCommandResponse
import java.util
import scala.collection.script.Update

// scalastyle:off number.of.methods

/**
 * A MongoDB Collection
 *
 * @param name The name of the collection
 * @param database The database that this collection belongs to
 * @param codec The collectable codec to use for operations
 * @param mongoCollectionOptions The options to use with this collection
 * @tparam T
 */
case class MongoCollection[T](name: String,
                              database: MongoDatabase,
                              codec: CollectibleCodec[T],
                              mongoCollectionOptions: MongoCollectionOptions) {

  /**
   * The MongoCollectionAdmin which provides admin methods for a collection
   */
  val admin = MongoCollectionAdmin[T](this)
  /**
   * The MongoClient
   */
  val client: MongoClient = database.client

  /**
   * The namespace for any operations
   */
  private[scala] val namespace: MongoNamespace = new MongoNamespace(database.name, name)

  /**
   * Insert a document into the database
   * @param document to be inserted
   */
  def insert(document: T): Future[WriteResult] = MongoCollectionView[T]().insert(document)

  /**
   * Insert a document into the database
   * @param documents the documents to be inserted
   */
  def insert(documents: Iterable[T]): Future[WriteResult] = MongoCollectionView[T]().insert(documents)

  /**
   * Count the number of documents
   */
  def count(): Future[Long] = MongoCollectionView[T]().count()

  /**
   * Get a cursor which is a [[https://github.com/Netflix/RxJava/wiki/Subject Subject]]
   */
  def cursor: Subject[T] = MongoCollectionView[T]().cursor

  /**
   * Return a list of results (blocking)
   */
  def toList: Future[List[T]] = MongoCollectionView[T]().toList

  /**
   * Run a transforming operation foreach resulting document.
   *
   * @param f the transforming function to apply to each document
   * @tparam A The resultant type
   * @return Observable[A]
   */
  def map[A](f: T => A): Observable[A] = MongoCollectionView[T]().map(f)

  def find(filter: ConvertibleToDocument): MongoCollectionView[T] = MongoCollectionView[T]().find(filter.toDocument)
  def find(filter: Document): MongoCollectionView[T] = MongoCollectionView[T]().find(filter)

  /**
   * Companion object for the chainable MongoCollectionView
   */
  protected object MongoCollectionView {

    /**
     * Creates the inital collection view - a find all.
     * @tparam D The document type
     * @return A composable MongoCollectionView
     */
    def apply[D](): MongoCollectionView[D] = {
      val findOp: Find = new Find()
      findOp.readPreference(mongoCollectionOptions.readPreference)
      val writeConcern: WriteConcern = mongoCollectionOptions.writeConcern
      MongoCollectionView[D](findOp, writeConcern, limitSet=false, doUpsert=false)
    }
  }

  protected case class MongoCollectionView[D](findOp: Find, writeConcern: WriteConcern,
                                              limitSet: Boolean, doUpsert: Boolean) extends HandleCommandResponse {
    /**
     * The document codec to use
     */
    val documentCodec = mongoCollectionOptions.documentCodec

    /**
     * The command codec to use
     */
    def getCodec: CollectibleCodec[D] = codec.asInstanceOf[CollectibleCodec[D]]

    /**
     * Insert a document into the database
     * @param document to be inserted
     */
    def insert(document: D): Future[WriteResult] = insert(List(document))

    /**
     * Insert a document into the database
     * @param documents the documents to be inserted
     */
    def insert(documents: Iterable[D]): Future[WriteResult] = {
      val insertRequestList = documents.map( new InsertRequest[D](_)).toList.asJava
      val operation = new InsertOperation[D](namespace, true, writeConcern, insertRequestList, getCodec, client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync)
    }

    /**
     * Count the number of documents
     */
    def count(): Future[Long] = {
      val operation = new CountOperation(namespace, findOp, database.documentCodec, client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync) map { result => result }
    }

    /**
     * Filter the collection
     * @param filter the query to perform
     */
    def find(filter: ConvertibleToDocument): MongoCollectionView[D] = find(filter.toDocument)

    /**
     * Filter the collection
     * @param filter the query to perform
     */
    def find(filter: Document): MongoCollectionView[D] = {
      findOp.filter(filter)
      this
    }

    /**
     * Return a list of results (blocking)
     */
    def toList: Future[List[D]] = Future(cursor.toBlockingObservable.toList)

    /**
     * Sort the results
     * @param sortCriteria the sort criteria
     */
    def sort(sortCriteria: ConvertibleToDocument): MongoCollectionView[D] = sort(sortCriteria.toDocument)

    /**
     * Sort the results
     * @param sortCriteria the sort criteria
     */
    def sort(sortCriteria: Document): MongoCollectionView[D] = {
      findOp.order(sortCriteria)
      this
    }

    /**
     * Fields to include / exclude with the output
     *
     * @param selector the fields to include / exclude
     */
    def fields(selector: ConvertibleToDocument): MongoCollectionView[D] = fields(selector.toDocument)
    /**
     * Fields to include / exclude with the output
     *
     * @param selector the fields to include / exclude
     */
    def fields(selector: Document): MongoCollectionView[D] = {
      findOp.select(selector)
      this
    }

    /**
     * Create a new document when no document matches the query criteria when doing an insert.
     */
    def upsert: MongoCollectionView[D] = MongoCollectionView(findOp, writeConcern, limitSet, doUpsert=true)

    /**
     * Set custom query options for the query
     * @param queryOptions the options to use for the cursor
     * @see [[http://docs.mongodb.org/manual/reference/method/cursor.addOption/#cursor-flags Cursor Options]]
     */
    def withQueryOptions(queryOptions: QueryOptions): MongoCollectionView[D] = {
      findOp.options(queryOptions)
      this
    }

    /**
     * Skip a number of documents
     * @param skip the number to skip
     */
    def skip(skip: Int): MongoCollectionView[D] = {
      findOp.skip(skip)
      this
    }

    /**
     * Limit the resulting results
     * @param limit the number to limit to
     */
    def limit(limit: Int): MongoCollectionView[D] = MongoCollectionView(findOp.limit(limit), writeConcern, limitSet=true, doUpsert)

    /**
     * Use a custom read preference to determine how MongoDB clients route read operations to members of a replica set
     * @param readPreference the read preference for the query
     * @see [[http://docs.mongodb.org/manual/core/read-preference read preference]]
     */
    def withReadPreference(readPreference: ReadPreference): MongoCollectionView[D] = {
      findOp.readPreference(readPreference)
      this
    }

    /**
     * Execute the operation and return the result.
     */
    def cursor: Subject[D] =
      new QueryOperation[D](namespace, findOp, documentCodec, getCodec, client.bufferProvider, client.session, false).cursor

    /**
     * Run a transforming operation foreach resulting document.
     *
     * @param f the transforming function to apply to each document
     * @tparam A The resultant type
     * @return Observable[A]
     */
    def map[A](f: D => A): Observable[A] = cursor.map(doc => f(doc))

    def get: Observable[D] = cursor
    def getOne: Observable[D] = copy().limit(1).cursor.first

//    def mapReduce(map: String, reduce: String): MongoIterable[T] = {
//      val commandOperation: MapReduceCommand = new MapReduceCommand(findOp, name, map, reduce)
//      val commandResult: CommandResult = database.execute(commandOperation.toDocument, commandOperation.getReadPreference)
//      new SingleShotCommandIterable[T](commandResult)
//    }

//    def into(target: A): A = {
//      forEach(new Block[T] {
//        def run(t: T): Boolean = {
//          target.add(t)
//          return true
//        }
//      })
//      return target
//    }

//    def map(mapper: Function[T, U]): MongoIterable[U] = {
//      return new MappingIterable[T, U](this, mapper)
//    }

    def withWriteConcern(writeConcernForThisOperation: WriteConcern): MongoCollectionView[T] =
      MongoCollectionView(findOp, writeConcernForThisOperation, limitSet, doUpsert)

    def save(document: D): Future[WriteResult] = {
      Option(getCodec.getId(document)) match {
        case None => insert(document)
        case Some(id) => upsert.find(new Document("_id", id)).replace(document)
      }
    }

    def remove(): Future[WriteResult] = {
      val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(getMultiFromLimit))
      val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec, client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync())
    }

    def removeOne(): Future[WriteResult] = {
      val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(false))
      val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec, client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync())
    }

    def update(updateOperations: ConvertibleToDocument): Future[WriteResult] = update(updateOperations.toDocument)
    def update(updateOperations: Document): Future[WriteResult] = {
      val updateRequest: List[UpdateRequest] = List(new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(getMultiFromLimit))
      val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec, client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync())
    }

    def updateOne(updateOperations: ConvertibleToDocument): Future[WriteResult] = updateOne(updateOperations.toDocument)
    def updateOne(updateOperations: Document): Future[WriteResult] = {
      val updateRequest: List[UpdateRequest] = List(new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(false))
      val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec,
                                          client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync())
    }

    def replace(replacement: D): Future[WriteResult] = {
      val replaceRequest: List[ReplaceRequest[D]] = List(new ReplaceRequest[D](findOp.getFilter, replacement).upsert(doUpsert))
      val operation = new ReplaceOperation(namespace, true, writeConcern, replaceRequest.asJava, documentCodec, getCodec,
                                           client.bufferProvider, client.session, false)
      MongoFuture(operation.executeAsync())
    }

    def updateOneAndGet(updateOperations: Document): Future[D] = updateOneAndGet(updateOperations, Get.AfterChangeApplied)
    def updateOneAndGet(updateOperations: ConvertibleToDocument): Future[D] = updateOneAndGet(updateOperations.toDocument)
    def updateOneAndGet(updateOperations: Document, beforeOrAfter: Get): Future[D] = {
      val findAndUpdate: FindAndUpdate[D] = new FindAndUpdate[D]()
        .where(findOp.getFilter)
        .updateWith(updateOperations)
        .returnNew(asBoolean(beforeOrAfter))
        .select(findOp.getFields)
        .sortBy(findOp.getOrder)
        .upsert(doUpsert)
      val operation = new FindAndUpdateOperation[D](namespace, findAndUpdate, getCodec, client.bufferProvider, client.session, false)
      // TODO needs executeAsync + MongoFuture
      Future(operation.execute())
    }

    def getOneAndUpdate(updateOperations: Document): Future[D] = updateOneAndGet(updateOperations, Get.BeforeChangeApplied)
    def getOneAndUpdate(updateOperations: ConvertibleToDocument): Future[D] = getOneAndUpdate(updateOperations.toDocument)

    def getOneAndReplace(replacement: D): Future[D] = replaceOneAndGet(replacement, Get.BeforeChangeApplied)

    def replaceOneAndGet(replacement: D): Future[D] = replaceOneAndGet(replacement, Get.AfterChangeApplied)
    def replaceOneAndGet(replacement: D, beforeOrAfter: Get): Future[D] = {
      val findAndReplace: FindAndReplace[D] = new FindAndReplace[D](replacement)
        .where(findOp.getFilter)
        .returnNew(asBoolean(beforeOrAfter))
        .select(findOp.getFields)
        .sortBy(findOp.getOrder)
        .upsert(doUpsert)
      val operation = new FindAndReplaceOperation[D](namespace, findAndReplace, getCodec, getCodec, client.bufferProvider, client.session, false)
      // TODO needs executeAsync + MongoFuture
      Future(operation.execute())
    }

    def getOneAndRemove: Future[D] = {
      val findAndRemove: FindAndRemove[D] = new FindAndRemove[D]().where(findOp.getFilter).select(findOp.getFields).sortBy(findOp.getOrder)
      val operation = new FindAndRemoveOperation[D](namespace, findAndRemove, getCodec, client.bufferProvider, client.session, false)
      // TODO needs executeAsync + MongoFuture
      Future(operation.execute())
    }

    private def asBoolean(get: Get): Boolean = get eq Get.AfterChangeApplied

    private def getMultiFromLimit: Boolean = {
      findOp.getLimit match {
        case 1 => false
        case 0 => true
        case _ => throw new IllegalArgumentException("Update currently only supports a limit of either none or 1")
      }
    }
  }
}

// scalastyle:on number.of.methods
