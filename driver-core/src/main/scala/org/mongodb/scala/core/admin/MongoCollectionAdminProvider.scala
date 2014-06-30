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
package org.mongodb.scala.core.admin

import java.lang.{Boolean => JBoolean}
import java.util

import org.bson.codecs.DecoderContext
import org.bson.{BsonDocumentReader, BsonDocumentWrapper, BsonDocument}
import org.mongodb.codecs.DocumentCodec
import org.mongodb.connection.SingleResultCallback
import org.mongodb.operation.{CommandReadOperation, CreateIndexesOperation, DropCollectionOperation, DropIndexOperation, GetIndexesOperation, SingleResultFuture}
import org.mongodb.scala.core.{MongoCollectionProvider, RequiredTypesAndTransformersProvider}
import org.mongodb.{CommandResult, Document, Index, MongoCommandFailureException, MongoException, MongoFuture, ReadPreference}

import scala.collection.JavaConverters._

/**
 * The MongoCollectionAdminProvider trait providing the core of a MongoCollectionAdmin implementation.
 *
 * To use the trait it requires a concrete implementation of [RequiredTypesAndTransformersProvider] to define the types the concrete
 * implementation uses..
 *
 * The core api remains the same between the implementations only the resulting types change based on the
 * [RequiredTypesAndTransformersProvider] implementation.
 *
 * {{{
 *    case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends MongoCollectionAdminProvider[T]
 *      with RequiredTypesAndTransformers
 * }}}
 *
 *
 * @tparam T the collection type
 */
trait MongoCollectionAdminProvider[T] {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * Drops the collection
   *
   * @return ResultType[Unit]
   */
  def drop(): ResultType[Unit] = {
    val operation = new DropCollectionOperation(collection.namespace)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * Is the collection capped
   * @return isCapped
   */
  def isCapped: ResultType[Boolean] = {
    val operation = createOperation(COLLECTION_STATS)
    val transformer = { result: MongoFuture[CommandResult] =>
      // Use native Java type to avoid Scala implicit conversion of null error if there's an exception
      val future: SingleResultFuture[JBoolean] = new SingleResultFuture[JBoolean]
      result.register(new SingleResultCallback[CommandResult] {
        def onResult(result: CommandResult, e: MongoException): Unit = {
          Option(e) match {
            case None =>
              val response: BsonDocument = result.getResponse
              future.init(response.containsKey("capped") && response.getBoolean("capped").getValue, null)
            case _ =>
              e.isInstanceOf[MongoCommandFailureException] match {
                case false => future.init(null, e)
                case true =>
                  val err = e.asInstanceOf[MongoCommandFailureException]
                  err.getCommandResult.getErrorMessage match {
                    case namespaceError: String if namespaceError.contains("not found") =>
                      future.init(false, null)
                    case _ => future.init(null, e)
                  }
              }
          }
        }
      })
      future
    }
    collection.client.executeAsync(operation, collection.options.readPreference, transformer).asInstanceOf[ResultType[Boolean]]
  }

  /**
   * Get statistics for the collection
   * @return ResultType[Document] of statistics
   */
  def statistics: ResultType[Document] = {
    val operation = createOperation(COLLECTION_STATS)
    val transformer = { result: MongoFuture[CommandResult] =>
      val future: SingleResultFuture[Document] = new SingleResultFuture[Document]
      result.register(new SingleResultCallback[CommandResult] {


        def onResult(result: CommandResult, e: MongoException): Unit = {
          Option(e) match {
            case None =>
              val stats: Document = bsonDocumentToDocument(result.getResponse)
              future.init(stats, null)
            case _ =>
              e.isInstanceOf[MongoCommandFailureException] match {
                case false => future.init(null, e)
                case true =>
                  val err = e.asInstanceOf[MongoCommandFailureException]
                  err.getCommandResult.getErrorMessage match {
                    case namespaceError: String if namespaceError.contains("not found") =>
                      future.init(bsonDocumentToDocument(err.getCommandResult.getResponse), null)
                    case _ => future.init(null, e)
                  }
            }
          }
        }
      })
      future
    }
    collection.client.executeAsync(operation, ReadPreference.primary(), transformer).asInstanceOf[ResultType[Document]]
  }

  /**
   * Create an index on the collection
   * @param index the index to be created
   * @return ResultType[Unit]
   */
  def createIndex(index: Index): ResultType[Unit] = createIndexes(List(index))

  /**
   * Create multiple indexes on the collection
   * @param indexes an iterable of indexes
   * @return ResultType[Unit]
   */
  def createIndexes(indexes: Iterable[Index]): ResultType[Unit] = {
    val operation = new CreateIndexesOperation(new util.ArrayList(indexes.toList.asJava), collection.namespace)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   * Get all the index information for this collection
   * @return ListResultType[Document]
   */
  def getIndexes: ListResultType[Document] = {
    val operation = new GetIndexesOperation(collection.namespace, commandCodec)
    val transformer: MongoFuture[util.List[Document]] => MongoFuture[List[Document]] = {
      result: MongoFuture[util.List[Document]] =>
      val future = new SingleResultFuture[List[Document]]
      result.register(new SingleResultCallback[util.List[Document]] {
        def onResult(result: util.List[Document], e: MongoException): Unit = {
          Option(e) match {
            case None => future.init(result.asScala.toList, null)
            case _ => future.init(null, e)
          }
        }
      })
      future
    }
    val result = collection.client.executeAsync(operation, collection.options.readPreference, transformer)
    listToListResultTypeConverter[Document](result.asInstanceOf[ResultType[List[Document]]])
  }

  /**
   * Drop an index from the collection
   * @param index the index name to be dropped
   * @return ResultType[Unit]
   */
  def dropIndex(index: String): ResultType[Unit] = {
    val operation = new DropIndexOperation(collection.namespace, index)
    voidToUnitConverter(collection.client.executeAsync(operation).asInstanceOf[ResultType[Void]])
  }

  /**
   *  Drop an index from the collection
   *
   * @param index the `Index` instance to drop
   * @return ResultType[Unit]
   */
  def dropIndex(index: Index): ResultType[Unit] = dropIndex(index.getName)

  /**
   * Drop all indexes from this collection
   * @return ResultType[Unit]
   */
  def dropIndexes(): ResultType[Unit] = dropIndex("*")

  /**
   * The collection which we administrating
   *
   * @note Its expected that the MongoCollectionAdmin implementation is a case class and this is the constructor params.
   */
  val collection: MongoCollectionProvider[T]

  /**
   * The Collection stats command document
   */
  private val COLLECTION_STATS = new Document("collStats", collection.name)

  private val commandCodec: DocumentCodec = new DocumentCodec()
  private val decoderContext: DecoderContext = DecoderContext.builder().build()
  private def createOperation(command: Document) = {
    new CommandReadOperation(collection.database.name, new BsonDocumentWrapper[Document](command, commandCodec))
  }

  private def bsonDocumentToDocument(bsonDocument: BsonDocument): Document =
    commandCodec.decode(new BsonDocumentReader(bsonDocument), decoderContext)

}
