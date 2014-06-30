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

import scala.collection.JavaConverters._
import scala.language.implicitConversions

import org.mongodb.{ Block, Document, MongoAsyncCursor, MongoException, MongoFuture, MongoNamespace, QueryOptions, ReadPreference, WriteConcern, WriteResult }
import org.mongodb.codecs.CollectibleCodec
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation._

import org.bson.{ BsonDocument, BsonDocumentWrapper }

/**
 * The MongoCollectionViewProvider trait providing the core of a MongoCollectionView implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the
 * concrete implementation uses.
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation. To do this the concrete implementation of this trait requires the following
 * methods to be implemented:
 *
 * {{{
 *    protected case class MongoCollectionView[T](client: MongoClient, namespace: MongoNamespace, codec: CollectibleCodec[T],
 *                                                options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern,
 *                                                limitSet: Boolean, doUpsert: Boolean, readPreference: ReadPreference)
 *      extends MongoCollectionViewProvider[T] with RequiredTypesAndTransformers {
 *
 *      protected def copy(client: MongoClient, namespace: MongoNamespace, codec: CollectibleCodec[T],
 *                         options: MongoCollectionOptions, findOp: Find, writeConcern: WriteConcern, limitSet: Boolean,
 *                         doUpsert: Boolean, readPreference: ReadPreference): MongoCollectionView[T] = MongoCollectionView[T]
 *    }
 * }}}
 *
 */
trait MongoCollectionViewProvider[T] {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * The MongoClient
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoCollection implementation
   */
  val client: Client

  /**
   * The MongoNamespace of the collection
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoCollection implementation
   */
  val namespace: MongoNamespace

  /**
   * The codec
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoCollection implementation
   */
  val codec: CollectibleCodec[T]

  /**
   * The MongoCollectionOptions
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoCollection implementation
   */
  val options: MongoCollectionOptions

  /**
   * The current Find operation
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   */
  val findOp: Find

  /**
   * The current WriteConcern
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   */
  val writeConcern: WriteConcern

  /**
   * Flag marking limit has been set
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   */
  val limitSet: Boolean

  /**
   * Flag marking upsert has been set
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   */
  val doUpsert: Boolean

  /**
   * The current ReadPreference
   *
   * @note Its expected that the MongoCollectionView implementation is a case class and this is one of the constructor params.
   */
  val readPreference: ReadPreference

  /**
   * The document codec to use
   */
  val documentCodec = options.documentCodec

  /**
   * The command codec to use
   */
  def getCodec: CollectibleCodec[T] = codec

  /**
   * Insert a document into the database
   * @param document to be inserted
   */
  def insert(document: T): ResultType[WriteResult] = insert(List(document))

  /**
   * Insert a document into the database
   * @param documents the documents to be inserted
   */
  def insert(documents: Iterable[T]): ResultType[WriteResult] = {
    val insertRequestList = documents.map(new InsertRequest[T](_)).toList.asJava
    val operation = new InsertOperation[T](namespace, true, writeConcern, insertRequestList, codec)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Count the number of documents
   */
  def count(): ResultType[Long] = {
    val operation = new CountOperation(namespace, findOp)
    client.executeAsync(operation, options.readPreference).asInstanceOf[ResultType[Long]]
  }

  /**
   * Filter the collection
   * @param filter the query to perform
   */
  def find(filter: Document): CollectionView[T] = copy(findOp = findOp.filter(filter))

  /**
   * Return a list of results (memory hungry)
   */
  def toList(): ResultType[List[T]] = {
    val operation = new QueryOperation[T](namespace, findOp, getCodec)
    val transformer = { result: MongoFuture[MongoAsyncCursor[T]] =>
      val future: SingleResultFuture[List[T]] = new SingleResultFuture[List[T]]
      var list = List[T]()
      result.register(new SingleResultCallback[MongoAsyncCursor[T]] {
        def onResult(cursor: MongoAsyncCursor[T], e: MongoException): Unit = {
          cursor.forEach(new Block[T] {
            override def apply(result: T): Unit = {
              list ::= result
            }
          }).register(new SingleResultCallback[Void] {
            def onResult(result: Void, e: MongoException) {
              future.init(list.reverse, null)
            }
          })
        }
      })
      future
    }
    client.executeAsync(operation, readPreference, transformer).asInstanceOf[ResultType[List[T]]]
  }

  /**
   * Sort the results
   * @param sortCriteria the sort criteria
   */
  def sort(sortCriteria: Document): CollectionView[T] = copy(findOp = findOp.order(sortCriteria))

  /**
   * Fields to include / exclude with the output
   *
   * @param selector the fields to include / exclude
   */
  def fields(selector: Document): CollectionView[T] = copy(findOp = findOp.select(selector))

  /**
   * Create a new document when no document matches the query criteria when doing an insert.
   */
  def upsert: CollectionView[T] = copy(doUpsert = true)

  /**
   * Set custom query options for the query
   * @param queryOptions the options to use for the cursor
   * @see [[http://docs.mongodb.org/manual/reference/method/cursor.addOption/#cursor-flags Cursor Options]]
   */
  def withQueryOptions(queryOptions: QueryOptions): CollectionView[T] = {
    copy().setQueryOptions(queryOptions).asInstanceOf[CollectionView[T]]
  }

  /**
   * Skip a number of documents
   * @param skip the number to skip
   */
  def skip(skip: Int): CollectionView[T] = copy(findOp = findOp.skip(skip))

  /**
   * Limit the resulting results
   * @param limit the number to limit to
   */
  def limit(limit: Int): CollectionView[T] = copy(findOp = findOp.limit(limit), limitSet = true)

  /**
   * Use a custom read preference to determine how MongoDB clients route read operations to members of a replica set
   * @param readPreference the read preference for the query
   * @see [[http://docs.mongodb.org/manual/core/read-preference read preference]]
   */
  def withReadPreference(readPreference: ReadPreference): CollectionView[T] =
    copy(readPreference = readPreference)

  /**
   * Execute the operation and return the result.
   *
   * @return CursorType[T]
   */
  def cursor(): CursorType[T] = {
    val operation = new QueryOperation[T](namespace, findOp, getCodec)
    client.executeAsync(operation, readPreference).asInstanceOf[CursorType[T]]
  }

  /**
   * Get the result from the query
   *
   * @return ResultType[Option[T\]\]
   */
  def one(): ResultType[Option[T]] = {
    val operation = new QueryOperation[T](namespace, limit(1).findOp, getCodec)
    val transformer = { result: MongoFuture[MongoAsyncCursor[T]] =>
      val future: SingleResultFuture[Option[T]] = new SingleResultFuture[Option[T]]
      var theOne: Option[T] = None
      result.register(new SingleResultCallback[MongoAsyncCursor[T]] {
        def onResult(cursor: MongoAsyncCursor[T], e: MongoException): Unit = {
          cursor.forEach(new Block[T] {
            override def apply(result: T): Unit = {
              theOne = Some(result)
            }
          }).register(new SingleResultCallback[Void] {
            def onResult(result: Void, e: MongoException) {
              future.init(theOne, null)
            }
          })
        }
      })
      future
    }
    client.executeAsync(operation, readPreference, transformer).asInstanceOf[ResultType[Option[T]]]
  }

  /**
   * Change the writeconcern
   *
   * @param writeConcernForThisOperation the new writeconcern
   */
  def withWriteConcern(writeConcernForThisOperation: WriteConcern): CollectionView[T] =
    copy(writeConcern = writeConcernForThisOperation)

  /**
   * Save a document
   *
   * Inserts if no `_id` or upserts and overwrites any existing documents
   *
   * @param document the document to save
   */
  def save(document: T): ResultType[WriteResult] = {
    Option(getCodec.getDocumentId(document)) match {
      case None     => insert(document)
      case Some(id) => upsert.find(new Document("_id", id)).replace(document).asInstanceOf[ResultType[WriteResult]]
    }
  }

  /**
   * Remove any matching documents
   */
  def remove(): ResultType[WriteResult] = {
    val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(getMultiFromLimit))
    val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Remove the first matching document
   */
  def removeOne(): ResultType[WriteResult] = {
    val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(false))
    val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Update matching documents
   *
   * @param updateOperations the update document
   */
  def update(updateOperations: Document): ResultType[WriteResult] = {
    val updateRequest: List[UpdateRequest] = List(
      new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(getMultiFromLimit))
    val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Update the first matching document
   *
   * @param updateOperations the update document
   */
  def updateOne(updateOperations: Document): ResultType[WriteResult] = {
    val updateRequest: List[UpdateRequest] = List(
      new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(false))
    val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Replace any matching documents
   *
   * @param replacement the replacement
   * @return
   */
  def replace(replacement: T): ResultType[WriteResult] = {
    val replaceRequest: List[ReplaceRequest[T]] = List(
      new ReplaceRequest[T](findOp.getFilter, replacement).upsert(doUpsert))
    val operation = new ReplaceOperation(namespace, true, writeConcern, replaceRequest.asJava, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Update a single document, then return it
   * @param updateOperations the update document
   * @return the updated document
   */
  def updateOneAndGet(updateOperations: Document): ResultType[T] = updateOneAndGet(updateOperations, returnNew = true)

  /**
   * Update a document and return either the original or updated document
   *
   * @param updateOperations the update document
   * @param returnNew return the updated document
   * @return document
   */
  def updateOneAndGet(updateOperations: Document, returnNew: Boolean): ResultType[T] = {
    val findAndUpdate: FindAndUpdate = new FindAndUpdate()
      .where(findOp.getFilter)
      .updateWith(updateOperations)
      .returnNew(returnNew)
      .select(findOp.getFields)
      .sortBy(findOp.getOrder)
      .upsert(doUpsert)
    val operation = new FindAndUpdateOperation[T](namespace, findAndUpdate, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[T]]
  }

  /**
   * Get a document and then update
   *
   * @param updateOperations the update document
   * @return the original document
   */
  def getOneAndUpdate(updateOperations: Document): ResultType[T] = updateOneAndGet(updateOperations, returnNew = false)

  /**
   * Get a doucment and then replace
   *
   * @param replacement the replacement document
   * @return the original document
   */
  def getOneAndReplace(replacement: T): ResultType[T] = replaceOneAndGet(replacement, returnNew = false)

  /**
   * Replace a document and return the updated document
   *
   * @param replacement the replacement document
   * @return the replaced document
   */
  def replaceOneAndGet(replacement: T): ResultType[T] = replaceOneAndGet(replacement, returnNew = true)

  /**
   * Replace a document and either return the original or replaced document
   *
   * @param replacement the replacement document
   * @param returnNew return the replaced document
   * @return document
   */
  def replaceOneAndGet(replacement: T, returnNew: Boolean): ResultType[T] = {
    val findAndReplace: FindAndReplace[T] = new FindAndReplace[T](replacement)
      .where(findOp.getFilter)
      .returnNew(returnNew)
      .select(findOp.getFields)
      .sortBy(findOp.getOrder)
      .upsert(doUpsert)
    val operation = new FindAndReplaceOperation[T](namespace, findAndReplace, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[T]]
  }

  /**
   * Get a document then remove it
   */
  def getOneAndRemove: ResultType[T] = {
    val findAndRemove: FindAndRemove[T] = new FindAndRemove[T]().where(findOp.getFilter).select(findOp.getFields).sortBy(findOp.getOrder)
    val operation = new FindAndRemoveOperation[T](namespace, findAndRemove, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[T]]
  }

  private def getMultiFromLimit: Boolean = {
    findOp.getLimit match {
      case 1 => false
      case 0 => true
      case _ => throw new IllegalArgumentException("Update currently only supports a limit of either none or 1")
    }
  }

  private def setQueryOptions(queryOptions: QueryOptions) = {
    this.findOp.options(queryOptions)
    this
  }

  private def copy(): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(findOp: Find): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(findOp: Find, limitSet: Boolean): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(doUpsert: Boolean): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(writeConcern: WriteConcern): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(readPreference: ReadPreference): CollectionView[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  /**
   * A copy method to produce a new updated version of a `CollectionView`
   *
   * @param client The MongoClient
   * @param namespace The MongoNamespace of the collection
   * @param codec The codec
   * @param options The MongoCollectionOptions
   * @param findOp The FindOp
   * @param writeConcern The current WriteConcern
   * @param limitSet flag indicating if [[limit]] has been called
   * @param doUpsert flag indicicating [[upsert]] has been called
   * @param readPreference the ReadPreference to use for this operation
   * @return an new CollectionView
   */
  protected def copy(client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T], options: MongoCollectionOptions,
                     findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean,
                     readPreference: ReadPreference): CollectionView[T]

  private implicit def wrap(command: Document): BsonDocument = {
    new BsonDocumentWrapper[Document](command, documentCodec)
  }

}
