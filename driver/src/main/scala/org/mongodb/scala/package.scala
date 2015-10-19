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

package org.mongodb

import _root_.scala.language.implicitConversions

import _root_.scala.reflect.ClassTag

import org.mongodb.scala.internal.WriteConcernImplicits

/**
 * The MongoDB Scala Driver package
 *
 * Contains type aliases and companion objects to help when using the Scala API
 *
 * @since 1.0
 */
package object scala extends ObservableImplicits with WriteConcernImplicits {

  /**
   * An immutable Document implementation.
   *
   * A strictly typed `Map[String, BsonValue]` like structure that traverses the elements in insertion order. Unlike native scala maps there
   * is no variance in the value type and it always has to be a `BsonValue`.
   */
  type Document = bson.Document

  /**
   * An immutable Document implementation.
   *
   * A strictly typed `Map[String, BsonValue]` like structure that traverses the elements in insertion order. Unlike native scala maps there
   * is no variance in the value type and it always has to be a `BsonValue`.
   */
  val Document = bson.Document

  /**
   * The result of a successful bulk write operation.
   */
  type BulkWriteResult = com.mongodb.bulk.BulkWriteResult

  /**
   * A MongoDB namespace, which includes a database name and collection name.
   */
  type MongoNamespace = com.mongodb.MongoNamespace

  /**
   * Represents preferred replica set members to which a query or command can be sent.
   */
  type ReadPreference = com.mongodb.ReadPreference
  /**
   * Represents ReadPreferences that can be combined with tags
   */
  type TaggableReadPreference = com.mongodb.TaggableReadPreference
  /**
   * A replica set tag
   */
  type Tag = com.mongodb.Tag
  /**
   * An immutable set of tags, used to select members of a replica set to use for read operations.
   */
  type TagSet = com.mongodb.TagSet
  /**
   * Controls the acknowledgment of write operations with various options.
   */
  type WriteConcern = com.mongodb.WriteConcern
  /**
   * The result of a successful write operation.  If the write was unacknowledged, then `wasAcknowledged` will return false and all
   * other methods with throw `MongoUnacknowledgedWriteException`.
   *
   * @see [[WriteConcern]]
   */
  type WriteConcernResult = com.mongodb.WriteConcernResult
  /**
   * Represents the details of a write error , e.g. a duplicate key error
   */
  type WriteError = com.mongodb.WriteError
  /**
   * Represents credentials to authenticate to a MongoDB server,as well as the source of the credentials and the authentication mechanism to
   * use.
   */
  type MongoCredential = com.mongodb.MongoCredential
  /**
   * Represents the location of a MongoDB server
   */
  type ServerAddress = com.mongodb.ServerAddress

  /**
   * Various settings to control the behavior of a `MongoClient`.
   */
  type MongoClientSettings = com.mongodb.async.client.MongoClientSettings

  // MongoException Aliases
  /**
   * Top level Exception for all Exceptions, server-side or client-side, that come from the driver.
   */
  type MongoException = com.mongodb.MongoException

  /**
   * An exception that represents all errors associated with a bulk write operation.
   */
  type MongoBulkWriteException = com.mongodb.MongoBulkWriteException

  /**
   * A base class for exceptions indicating a failure condition with the MongoClient.
   */
  type MongoClientException = com.mongodb.MongoClientException

  /**
   * An exception indicating that a command sent to a MongoDB server returned a failure.
   */
  type MongoCommandException = com.mongodb.MongoCommandException

  /**
   * Subclass of [[MongoException]] representsing a cursor-not-found exception.
   */
  type MongoCursorNotFoundException = com.mongodb.MongoCursorNotFoundException

  /**
   * Exception indicating that the execution of the current operation timed out as a result of the maximum operation time being exceeded.
   */
  type MongoExecutionTimeoutException = com.mongodb.MongoExecutionTimeoutException

  /**
   * An exception indicating that this version of the driver is not compatible with at least one of the servers that it is currently
   * connected to.
   */
  type MongoIncompatibleDriverException = com.mongodb.MongoIncompatibleDriverException

  /**
   * A Mongo exception internal to the driver, not carrying any error code.
   */
  type MongoInternalException = com.mongodb.MongoInternalException

  /**
   * A non-checked exception indicating that the driver has been interrupted by a call to `Thread.interrupt`.
   */
  type MongoInterruptedException = com.mongodb.MongoInterruptedException

  /**
   * An exception indicating that the server is a member of a replica set but is in recovery mode, and therefore refused to execute
   * the operation. This can happen when a server is starting up and trying to join the replica set.
   */
  type MongoNodeIsRecoveringException = com.mongodb.MongoNodeIsRecoveringException

  /**
   * An exception indicating that the server is a member of a replica set but is not the primary, and therefore refused to execute either a
   * write operation or a read operation that required a primary.  This can happen during a replica set election.
   */
  type MongoNotPrimaryException = com.mongodb.MongoNotPrimaryException

  /**
   * An exception indicating that a query operation failed on the server.
   */
  type MongoQueryException = com.mongodb.MongoQueryException

  /**
   * This exception is thrown when there is an error reported by the underlying client authentication mechanism.
   */
  type MongoSecurityException = com.mongodb.MongoSecurityException

  /**
   * An exception indicating that some error has been raised by a MongoDB server in response to an operation.
   */
  type MongoServerException = com.mongodb.MongoServerException

  /**
   * This exception is thrown when trying to read or write from a closed socket.
   */
  type MongoSocketClosedException = com.mongodb.MongoSocketClosedException

  /**
   * Subclass of [[MongoException]] representing a network-related exception
   */
  type MongoSocketException = com.mongodb.MongoSocketException

  /**
   * This exception is thrown when there is an exception opening a Socket.
   */
  type MongoSocketOpenException = com.mongodb.MongoSocketOpenException

  /**
   * This exception is thrown when there is an exception reading a response from a Socket.
   */
  type MongoSocketReadException = com.mongodb.MongoSocketReadException

  /**
   * This exception is thrown when there is a timeout reading a response from the socket.
   */
  type MongoSocketReadTimeoutException = com.mongodb.MongoSocketReadTimeoutException

  /**
   * This exception is thrown when there is an exception writing a response to a Socket.
   */
  type MongoSocketWriteException = com.mongodb.MongoSocketWriteException

  /**
   * An exception indicating that the driver has timed out waiting for either a server or a connection to become available.
   */
  type MongoTimeoutException = com.mongodb.MongoTimeoutException

  /**
   * An exception indicating that the queue for waiting for a pooled connection is full.
   *
   * @see [[http://api.mongodb.org/java/current/com/mongodb/connection/ConnectionPoolSettings.html#getMaxWaitQueueSize--]]
   */
  type MongoWaitQueueFullException = com.mongodb.MongoWaitQueueFullException

  /**
   * An exception indicating a failure to apply the write concern to the requested write operation
   *
   * @see [[WriteConcern]]
   */
  type MongoWriteConcernException = com.mongodb.MongoWriteConcernException

  /**
   * An exception indicating the failure of a write operation.
   */
  type MongoWriteException = com.mongodb.MongoWriteException

  /**
   * An exception representing an error reported due to a write failure.
   */
  type WriteConcernException = com.mongodb.WriteConcernException

  /**
   * Subclass of [[WriteConcernException]] representing a duplicate key exception
   */
  type DuplicateKeyException = com.mongodb.DuplicateKeyException

  /**
   * Helper to get the class from a classTag
   *
   * @param ct the classTag we want to implicitly get the class of
   * @tparam C the class type
   * @return the classOf[C]
   */
  implicit def classTagToClassOf[C](ct: ClassTag[C]): Class[C] = ct.runtimeClass.asInstanceOf[Class[C]]
}
