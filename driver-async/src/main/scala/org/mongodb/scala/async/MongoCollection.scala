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
package org.mongodb.scala.async

import scala.Some
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import org.mongodb.{Block, CollectibleCodec, ConvertibleToDocument, Document, MongoAsyncCursor, MongoException,
                    MongoNamespace, QueryOptions, ReadPreference, WriteConcern, WriteResult}
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.{CountOperation, Find, FindAndRemove, FindAndRemoveOperation, FindAndReplace,
                              FindAndReplaceOperation, FindAndUpdate, FindAndUpdateOperation, InsertOperation,
                              InsertRequest, QueryOperation, RemoveOperation, RemoveRequest, ReplaceOperation,
                              ReplaceRequest, UpdateOperation, UpdateRequest}

import org.mongodb.scala.core.MongoCollectionOptions
import org.mongodb.scala.async.admin.MongoCollectionAdmin
import org.mongodb.scala.async.utils.HandleCommandResponse

// scalastyle:off number.of.methods

/**
 * A MongoDB Collection
 *
 * @param name The name of the collection
 * @param database The database that this collection belongs to
 * @param codec The collectable codec to use for operations
 * @param options The options to use with this collection
 * @tparam T
 */
case class MongoCollection[T](name: String,
                              database: MongoDatabase,
                              codec: CollectibleCodec[T],
                              options: MongoCollectionOptions) {

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
  def cursor(): Future[MongoAsyncCursor[T]] = MongoCollectionView[T]().cursor()

  /**
   * Return a list of results (memory hungry)
   */
  def toList(): Future[List[T]] = MongoCollectionView[T]().toList()

  //  /**
  //   * Run a transforming operation foreach resulting document.
  //   *
  //   * @param f the transforming function to apply to each document
  //   * @tparam A The resultant type
  //   * @return Observable[A]
  //   */
  //  def map[A](f: T => A): Observable[A] = MongoCollectionView[T]().map(f)

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
      MongoCollectionView[D](findOp, options.writeConcern, limitSet = false, doUpsert = false, options.readPreference)
    }
  }


  protected case class MongoCollectionView[D](findOp: Find, writeConcern: WriteConcern,
                                              limitSet: Boolean, doUpsert: Boolean,
                                              readPreference: ReadPreference) extends HandleCommandResponse {
    /**
     * The document codec to use
     */
    val documentCodec = options.documentCodec

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
      val insertRequestList = documents.map(new InsertRequest[D](_)).toList.asJava
      val operation = new InsertOperation[D](namespace, true, writeConcern, insertRequestList, getCodec)
      client.executeAsync(operation)
    }

    /**
     * Count the number of documents
     */
    def count(): Future[Long] = {
      val operation = new CountOperation(namespace, findOp, database.documentCodec)
      client.executeAsync(operation, options.readPreference).asInstanceOf[Future[Long]]
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
      copy(findOp = findOp.filter(filter))
    }

    /**
     * Return a list of results (memory hungry)
     */
    def toList(): Future[List[D]] = {
      val promise = Promise[List[D]]()
      var list = List[D]()
      val futureCursor: Future[MongoAsyncCursor[D]] = cursor()
      futureCursor.onComplete({
        case Success(cursor) =>
          cursor.forEach(new Block[D] {
            override def apply(d: D): Unit = {
              list ::= d
            }
          }).register(new SingleResultCallback[Void] {
            def onResult(result: Void, e: MongoException) {
              if (e != null) promise.failure(e)
              else promise.success(list.reverse)
            }
          })
        case Failure(e) => promise.failure(e)
      })
      promise.future
    }

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
      copy(findOp = findOp.order(sortCriteria))
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
    def fields(selector: Document): MongoCollectionView[D] = copy(findOp = findOp.select(selector))

    /**
     * Create a new document when no document matches the query criteria when doing an insert.
     */
    def upsert: MongoCollectionView[D] = copy(doUpsert = true)

    /**
     * Set custom query options for the query
     * @param queryOptions the options to use for the cursor
     * @see [[http://docs.mongodb.org/manual/reference/method/cursor.addOption/#cursor-flags Cursor Options]]
     */
    def withQueryOptions(queryOptions: QueryOptions): MongoCollectionView[D] = {
      val view: MongoCollectionView[D] = copy()
      view.findOp.options(queryOptions)
      view
    }

    /**
     * Skip a number of documents
     * @param skip the number to skip
     */
    def skip(skip: Int): MongoCollectionView[D] = copy(findOp = findOp.skip(skip))

    /**
     * Limit the resulting results
     * @param limit the number to limit to
     */
    def limit(limit: Int): MongoCollectionView[D] = copy(findOp = findOp.limit(limit), limitSet = true)

    /**
     * Use a custom read preference to determine how MongoDB clients route read operations to members of a replica set
     * @param readPreference the read preference for the query
     * @see [[http://docs.mongodb.org/manual/core/read-preference read preference]]
     */
    def withReadPreference(readPreference: ReadPreference): MongoCollectionView[D] =
      copy(readPreference = readPreference)

    /**
     * Execute the operation and return the result.
     */
    def cursor(): Future[MongoAsyncCursor[D]] = {
      val operation = new QueryOperation[D](namespace, findOp, documentCodec, getCodec)
      client.executeAsync(operation, readPreference)
    }

    //    /**
    //     * Run a transforming operation foreach resulting document.
    //     *
    //     * @param f the transforming function to apply to each document
    //     * @tparam A The resultant type
    //     * @return Observable[A]
    //     */
    //    def map[A](f: D => A): Future[MongoAsyncCursor[A]] = cursor.map(doc => f(doc))

    def one(): Future[Option[D]] = {
      val promise = Promise[Option[D]]()
      val futureCursor: Future[MongoAsyncCursor[D]] = limit(1).cursor
      futureCursor.onComplete({
        case Success(cursor) =>
          cursor.forEach(new Block[D] {
            override def apply(d: D): Unit = {
              if (!promise.future.isCompleted) {
                promise.success(Some(d))
              }
            }
          }).register(new SingleResultCallback[Void] {
            def onResult(result: Void, e: MongoException) {
              if (!promise.future.isCompleted) {
                promise.success(None)
              }
            }
          })
        case Failure(e) => promise.failure(e)
      })
      promise.future
    }

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
      copy(writeConcern = writeConcernForThisOperation)

    def save(document: D): Future[WriteResult] = {
      Option(getCodec.getId(document)) match {
        case None => insert(document)
        case Some(id) => upsert.find(new Document("_id", id)).replace(document)
      }
    }

    def remove(): Future[WriteResult] = {
      val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(getMultiFromLimit))
      val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec)
      client.executeAsync(operation)
    }

    def removeOne(): Future[WriteResult] = {
      val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(false))
      val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec)
      client.executeAsync(operation)
    }

    def update(updateOperations: ConvertibleToDocument): Future[WriteResult] = update(updateOperations.toDocument)

    def update(updateOperations: Document): Future[WriteResult] = {
      val updateRequest: List[UpdateRequest] = List(new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(getMultiFromLimit))
      val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
      client.executeAsync(operation)
    }

    def updateOne(updateOperations: ConvertibleToDocument): Future[WriteResult] = updateOne(updateOperations.toDocument)

    def updateOne(updateOperations: Document): Future[WriteResult] = {
      val updateRequest: List[UpdateRequest] = List(new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(false))
      val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
      client.executeAsync(operation)
    }

    def replace(replacement: D): Future[WriteResult] = {
      val replaceRequest: List[ReplaceRequest[D]] = List(new ReplaceRequest[D](findOp.getFilter, replacement).upsert(doUpsert))
      val operation = new ReplaceOperation(namespace, true, writeConcern, replaceRequest.asJava, documentCodec, getCodec)
      client.executeAsync(operation)
    }

    def updateOneAndGet(updateOperations: Document): Future[D] = updateOneAndGet(updateOperations, returnNew = true)

    def updateOneAndGet(updateOperations: ConvertibleToDocument): Future[D] = updateOneAndGet(updateOperations.toDocument)

    def updateOneAndGet(updateOperations: Document, returnNew: Boolean): Future[D] = {
      val findAndUpdate: FindAndUpdate = new FindAndUpdate()
        .where(findOp.getFilter)
        .updateWith(updateOperations)
        .returnNew(returnNew)
        .select(findOp.getFields)
        .sortBy(findOp.getOrder)
        .upsert(doUpsert)
      val operation = new FindAndUpdateOperation[D](namespace, findAndUpdate, getCodec)
      client.executeAsync(operation)
    }

    def getOneAndUpdate(updateOperations: Document): Future[D] = updateOneAndGet(updateOperations, returnNew = false)

    def getOneAndUpdate(updateOperations: ConvertibleToDocument): Future[D] = getOneAndUpdate(updateOperations.toDocument)

    def getOneAndReplace(replacement: D): Future[D] = replaceOneAndGet(replacement, returnNew = false)

    def replaceOneAndGet(replacement: D): Future[D] = replaceOneAndGet(replacement, returnNew = true)

    def replaceOneAndGet(replacement: D, returnNew: Boolean): Future[D] = {
      val findAndReplace: FindAndReplace[D] = new FindAndReplace[D](replacement)
        .where(findOp.getFilter)
        .returnNew(returnNew)
        .select(findOp.getFields)
        .sortBy(findOp.getOrder)
        .upsert(doUpsert)
      val operation = new FindAndReplaceOperation[D](namespace, findAndReplace, getCodec, getCodec)
      client.executeAsync(operation)
    }

    def getOneAndRemove: Future[D] = {
      val findAndRemove: FindAndRemove[D] = new FindAndRemove[D]().where(findOp.getFilter).select(findOp.getFields).sortBy(findOp.getOrder)
      val operation = new FindAndRemoveOperation[D](namespace, findAndRemove, getCodec)
      client.executeAsync(operation)
    }

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
