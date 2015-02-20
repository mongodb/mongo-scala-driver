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

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.reactivestreams.client.{ MongoDatabase => JMongoDatabase }
import com.mongodb.scala.reactivestreams.client.Helpers.{ DefaultsTo, classTagToClassOf }
import com.mongodb.{ ReadPreference, WriteConcern }
import com.mongodb.scala.reactivestreams.client.collection.Document
import org.bson.codecs.configuration.CodecRegistry
import org.reactivestreams.Publisher

import scala.reflect.ClassTag

/**
 * The MongoDatabase representation.
 *
 * @param wrapped the underlying java MongoDatabase
 */
case class MongoDatabase(private val wrapped: JMongoDatabase) {

  /**
   * Gets the name of the database.
   *
   * @return the database name
   */
  lazy val name: String = wrapped.getName

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
   * Create a new MongoDatabase instance with a different codec registry.
   *
   * @param codecRegistry the new { @link org.bson.codecs.configuration.CodecRegistry} for the collection
   * @return a new MongoDatabase instance with the different codec registry
   */
  def withCodecRegistry(codecRegistry: CodecRegistry): MongoDatabase =
    MongoDatabase(wrapped.withCodecRegistry(codecRegistry))

  /**
   * Create a new MongoDatabase instance with a different read preference.
   *
   * @param readPreference the new { @link com.mongodb.ReadPreference} for the collection
   * @return a new MongoDatabase instance with the different readPreference
   */
  def withReadPreference(readPreference: ReadPreference): MongoDatabase =
    MongoDatabase(wrapped.withReadPreference(readPreference))

  /**
   * Create a new MongoDatabase instance with a different write concern.
   *
   * @param writeConcern the new { @link com.mongodb.WriteConcern} for the collection
   * @return a new MongoDatabase instance with the different writeConcern
   */
  def withWriteConcern(writeConcern: WriteConcern): MongoDatabase =
    MongoDatabase(wrapped.withWriteConcern(writeConcern))

  /**
   * Gets a collection, with a specific default document class.
   *
   * @param collectionName the name of the collection to return
   * @tparam T            the type of the class to use instead of { @code Document}.
   * @return the collection
   */
  def getCollection[T](collectionName: String)(implicit e: T DefaultsTo Document, ct: ClassTag[T]): MongoCollection[T] =
    MongoCollection(wrapped.getCollection(collectionName, ct.runtimeClass.asInstanceOf[Class[T]]))

  /**
   * Executes command in the context of the current database.
   *
   * @param command  the command to be run
   * @tparam T      the type of the class to use instead of { @code Document}.
   * @return a publisher containing the command result
   */
  def executeCommand[T](command: AnyRef)(implicit e: T DefaultsTo Document, ct: ClassTag[T]): Publisher[T] =
    wrapped.executeCommand[T](command, ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Executes command in the context of the current database.
   *
   * @param command        the command to be run
   * @param readPreference the { @link com.mongodb.ReadPreference} to be used when executing the command
   * @tparam T            the type of the class to use instead of { @code Document}.
   * @return a publisher containing the command result
   */
  def executeCommand[T](command: AnyRef, readPreference: ReadPreference)(implicit e: T DefaultsTo Document, ct: ClassTag[T]): Publisher[T] =
    wrapped.executeCommand(command, readPreference, ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Drops this database.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/dropDatabase/#dbcmd.dropDatabase Drop database]]
   * @return a publisher identifying when the database has been dropped
   */
  def dropDatabase(): Publisher[Void] = wrapped.dropDatabase()

  /**
   * Gets the names of all the collections in this database.
   *
   * @return a publisher with all the names of all the collections in this database
   */
  def listCollectionNames(): Publisher[String] = wrapped.listCollectionNames()

  /**
   * Finds all the collections in this database.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listCollections listCollections]]
   * @tparam T   the target document type of the iterable.
   * @return the fluent list collections interface
   */
  def listCollections[T]()(implicit e: T DefaultsTo Document, ct: ClassTag[T]): ListCollectionsPublisher[T] =
    ListCollectionsPublisher(wrapped.listCollections(ct))

  /**
   * Create a new collection with the given name.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param collectionName the name for the new collection to create
   * @return a publisher identifying when the collection has been created
   */
  def createCollection(collectionName: String): Publisher[Void] = wrapped.createCollection(collectionName)

  /**
   * Create a new collection with the selected options
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param collectionName the name for the new collection to create
   * @param options        various options for creating the collection
   * @return a publisher identifying when the collection has been created
   */
  def createCollection(collectionName: String, options: CreateCollectionOptions): Publisher[Void] =
    wrapped.createCollection(collectionName, options)
}
