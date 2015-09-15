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

package org.mongodb.scala

/**
 * The connection package contains classes that manage connecting to MongoDB servers.
 */
package object connection {

  /**
   * Settings for the cluster.
   */
  type ClusterSettings = com.mongodb.connection.ClusterSettings

  /**
   * All settings that relate to the pool of connections to a MongoDB server.
   */
  type ConnectionPoolSettings = com.mongodb.connection.ConnectionPoolSettings

  /**
   * Settings relating to monitoring of each server.
   */
  type ServerSettings = com.mongodb.connection.ServerSettings

  /**
   * An immutable class representing socket settings used for connections to a MongoDB server.
   */
  type SocketSettings = com.mongodb.connection.SocketSettings

  /**
   * Settings for connecting to MongoDB via SSL.
   */
  type SslSettings = com.mongodb.connection.SslSettings

  /**
   * The factory for streams.
   */
  type StreamFactory = com.mongodb.connection.StreamFactory

  /**
   * A factory of `StreamFactory` instances.
   */
  type StreamFactoryFactory = com.mongodb.connection.StreamFactoryFactory

  /**
   * A `StreamFactoryFactory` implementation for AsynchronousSocketChannel-based streams.
   *
   * @see java.nio.channels.AsynchronousSocketChannel
   */
  type AsynchronousSocketChannelStreamFactoryFactory = com.mongodb.connection.AsynchronousSocketChannelStreamFactoryFactory

}
