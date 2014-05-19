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

import org.mongodb._

import org.mongodb.scala.core.admin.MongoCollectionAdminProvider

trait MongoCollectionProvider[T] {

  this: RequiredTypesProvider =>

  val name: String
  val database: Database
  val codec: CollectibleCodec[T]
  val options: MongoCollectionOptions

  /**
   * The MongoCollectionAdmin which provides admin methods for a collection
   */
  val admin: MongoCollectionAdminProvider[T]

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

  def find(filter: Document): CollectionView[T] = collectionView.find(filter).asInstanceOf[CollectionView[T]]

  protected def collectionView: MongoCollectionViewProvider[T]
}
