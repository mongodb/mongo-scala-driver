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

import org.mongodb.{CollectibleCodec, Document, MongoNamespace, WriteResult}

import org.mongodb.scala.core.admin.MongoCollectionAdminProvider

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

 *    }
 * }}}
 *
 */
trait MongoCollectionProvider[T] {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * A concrete implementation of [[MongoCollectionAdminProvider]]
   *
   * @note Each MongoCollection implementation must provide this.
   */
  val admin: MongoCollectionAdminProvider[T]

  /**
   * A concrete implementation of [[MongoCollectionViewProvider]] to provide the MongoCollectionView for this
   * MongoCollection
   *
   * @note Each MongoCollection implementation must provide this.
   */
  protected def collectionView: MongoCollectionViewProvider[T]

  /**
   * The name of the collection
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val name: String

  /**
   * The MongoDatabase
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val database: Database

  /**
   * The codec that is used with this collection
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val codec: CollectibleCodec[T]

  /**
   * The MongoCollectionOptions to be used with this MongoCollection instance
   *
   * @note Its expected that the MongoCollection implementation is a case class and this is one of the constructor params.
   *       This is passed in from the MongoDatabase Implementation
   */
  val options: MongoCollectionOptions

  /**
   * The MongoClient
   */
  val client: Client = database.client.asInstanceOf[Client]

  /**
   * The namespace for any operations
   */
  private[scala] val namespace: MongoNamespace = new MongoNamespace(database.name, name)

  /**
   * Insert a document into the database
   * @param document to be inserted
   */
  def insert(document: T): ResultType[WriteResult] = insert(List(document))

  /**
   * Insert a document into the database
   * @param documents the documents to be inserted
   */
  def insert(documents: Iterable[T]): ResultType[WriteResult] =
    collectionView.insert(documents).asInstanceOf[ResultType[WriteResult]]

  /**
   * Count the number of documents
   */
  def count(): ResultType[Long] = collectionView.count().asInstanceOf[ResultType[Long]]

  /**
   * Get a cursor
   */
  def cursor(): ResultType[CursorType[T]] = collectionView.cursor().asInstanceOf[ResultType[CursorType[T]]]

  /**
   * Return a list of results (memory hungry)
   */
  def toList(): ResultType[List[T]] = collectionView.toList().asInstanceOf[ResultType[List[T]]]

  /**
   * Find documents in this collection
   * @param filter the filter to be used to find the documents
   * @return the collection view for further chaining
   */
  def find(filter: Document): CollectionView[T] = collectionView.find(filter).asInstanceOf[CollectionView[T]]

}
