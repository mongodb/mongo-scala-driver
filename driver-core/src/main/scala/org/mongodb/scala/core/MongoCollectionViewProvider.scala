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
import _root_.scala.Some
import _root_.scala.collection.JavaConverters._

import _root_.scala.Some
import org.mongodb._
import org.mongodb.operation._

/**
 * A MongoDB Collection
 */
trait MongoCollectionViewProvider[T] {

  this: RequiredTypesProvider =>

  val findOp: Find
  val writeConcern: WriteConcern
  val limitSet: Boolean
  val doUpsert: Boolean
  val readPreference: ReadPreference

  val client: Client
  val namespace: MongoNamespace
  val codec: CollectibleCodec[T]
  val options: MongoCollectionOptions

  /**
   * The document codec to use
   */
  val documentCodec = options.documentCodec

  /**
   * The command codec to use
   */
  def getCodec: CollectibleCodec[T] = codec.asInstanceOf[CollectibleCodec[T]] // Compile Fix

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
    val operation = new InsertOperation[T](namespace, true, writeConcern, insertRequestList, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[WriteResult]]
  }

  /**
   * Count the number of documents
   */
  def count() = {
    val operation = new CountOperation(namespace, findOp, documentCodec)
    client.executeAsync(operation, options.readPreference)
  }

  /**
   * Filter the collection
   * @param filter the query to perform
   */
  def find(filter: Document): MongoCollectionViewProvider[T] = copy(findOp = findOp.filter(filter))

  /**
   * Return a list of results (memory hungry)
   */
  def toList(): ResultType[List[T]]

  /**
   * Sort the results
   * @param sortCriteria the sort criteria
   */
  def sort(sortCriteria: ConvertibleToDocument): MongoCollectionViewProvider[T] = sort(sortCriteria.toDocument)

  /**
   * Sort the results
   * @param sortCriteria the sort criteria
   */
  def sort(sortCriteria: Document): MongoCollectionViewProvider[T] = copy(findOp = findOp.order(sortCriteria))

  /**
   * Fields to include / exclude with the output
   *
   * @param selector the fields to include / exclude
   */
  def fields(selector: Document): MongoCollectionViewProvider[T] = copy(findOp = findOp.select(selector))

  /**
   * Create a new document when no document matches the query criteria when doing an insert.
   */
  def upsert: MongoCollectionViewProvider[T] = copy(doUpsert = true)

  /**
   * Set custom query options for the query
   * @param queryOptions the options to use for the cursor
   * @see [[http://docs.mongodb.org/manual/reference/method/cursor.addOption/#cursor-flags Cursor Options]]
   */
  def withQueryOptions(queryOptions: QueryOptions): MongoCollectionViewProvider[T] = {
    create().setQueryOptions(queryOptions)
  }

  /**
   * Skip a number of documents
   * @param skip the number to skip
   */
  def skip(skip: Int): MongoCollectionViewProvider[T] = copy(findOp = findOp.skip(skip))

  /**
   * Limit the resulting results
   * @param limit the number to limit to
   */
  def limit(limit: Int): MongoCollectionViewProvider[T] = copy(findOp = findOp.limit(limit), limitSet = true)

  /**
   * Use a custom read preference to determine how MongoDB clients route read operations to members of a replica set
   * @param readPreference the read preference for the query
   * @see [[http://docs.mongodb.org/manual/core/read-preference read preference]]
   */
  def withReadPreference(readPreference: ReadPreference): MongoCollectionViewProvider[T] =
    copy(readPreference = readPreference)

  /**
   * Execute the operation and return the result.
   */
  def cursor(): ResultType[CursorType[T]] =  {
    val operation = new QueryOperation[T](namespace, findOp, documentCodec, getCodec)
    client.executeAsync(operation, readPreference).asInstanceOf[ResultType[CursorType[T]]]
  }

  def one(): ResultType[Option[T]]

  def withWriteConcern(writeConcernForThisOperation: WriteConcern): MongoCollectionViewProvider[T] =
    copy(writeConcern = writeConcernForThisOperation)

  def save(document: T): ResultType[WriteResult] = {
    Option(getCodec.getId(document)) match {
      case None => insert(document).asInstanceOf[ResultType[WriteResult]]
      case Some(id) => upsert.find(new Document("_id", id)).replace(document).asInstanceOf[ResultType[WriteResult]]
    }
  }

  def remove() = {
    val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(getMultiFromLimit))
    val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec)
    client.executeAsync(operation)
  }

  def removeOne() = {
    val removeRequest: List[RemoveRequest] = List(new RemoveRequest(findOp.getFilter).multi(false))
    val operation = new RemoveOperation(namespace, true, writeConcern, removeRequest.asJava, documentCodec)
    client.executeAsync(operation)
  }

  def update(updateOperations: Document) = {
    val updateRequest: List[UpdateRequest] = List(
      new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(getMultiFromLimit)
    )
    val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
    client.executeAsync(operation)
  }

  def updateOne(updateOperations: Document) = {
    val updateRequest: List[UpdateRequest] = List(
      new UpdateRequest(findOp.getFilter, updateOperations).upsert(doUpsert).multi(false)
    )
    val operation = new UpdateOperation(namespace, true, writeConcern, updateRequest.asJava, documentCodec)
    client.executeAsync(operation)
  }

  def replace(replacement: T) = {
    val replaceRequest: List[ReplaceRequest[T]] = List(
      new ReplaceRequest[T](findOp.getFilter, replacement).upsert(doUpsert)
    )
    val operation = new ReplaceOperation(namespace, true, writeConcern, replaceRequest.asJava, documentCodec, getCodec)
    client.executeAsync(operation)
  }

  def updateOneAndGet(updateOperations: Document): ResultType[T] = updateOneAndGet(updateOperations, returnNew = true)

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

  def getOneAndUpdate(updateOperations: Document): ResultType[T] = updateOneAndGet(updateOperations, returnNew = false)

  def getOneAndReplace(replacement: T): ResultType[T] = replaceOneAndGet(replacement, returnNew = false)

  def replaceOneAndGet(replacement: T): ResultType[T] = replaceOneAndGet(replacement, returnNew = true)

  def replaceOneAndGet(replacement: T, returnNew: Boolean): ResultType[T] = {
    val findAndReplace: FindAndReplace[T] = new FindAndReplace[T](replacement)
      .where(findOp.getFilter)
      .returnNew(returnNew)
      .select(findOp.getFields)
      .sortBy(findOp.getOrder)
      .upsert(doUpsert)
    val operation = new FindAndReplaceOperation[T](namespace, findAndReplace, getCodec, getCodec)
    client.executeAsync(operation).asInstanceOf[ResultType[T]]
  }

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

  private def setQueryOptions(queryOptions: QueryOptions): MongoCollectionViewProvider[T] = {
    this.findOp.options(queryOptions)
    this
  }
  
  private def create(): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(findOp: Find): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(findOp: Find, limitSet: Boolean): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(doUpsert: Boolean): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(writeConcern: WriteConcern): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  private def copy(readPreference: ReadPreference): MongoCollectionViewProvider[T] = {
    copy(client, namespace, codec, options, findOp, writeConcern, limitSet, doUpsert, readPreference)
  }

  protected def copy(client: Client, namespace: MongoNamespace, codec: CollectibleCodec[T], options: MongoCollectionOptions,
           findOp: Find, writeConcern: WriteConcern, limitSet: Boolean, doUpsert: Boolean,
           readPreference: ReadPreference): MongoCollectionViewProvider[T]
}
