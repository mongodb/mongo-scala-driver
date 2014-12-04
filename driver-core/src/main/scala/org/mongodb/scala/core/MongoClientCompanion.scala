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

import com.mongodb.ReadPreference.primary
import com.mongodb.client.options.OperationOptions
import org.bson.codecs.{ BsonValueCodecProvider, DocumentCodecProvider, ValueCodecProvider }
import org.bson.codecs.configuration.RootCodecRegistry

import scala.Some
import scala.collection.JavaConverters._

import com.mongodb.{ WriteConcern, MongoCredential, ServerAddress }
import com.mongodb.connection.{
  AsynchronousSocketChannelStreamFactory,
  BufferProvider,
  Cluster,
  ClusterConnectionMode,
  ClusterSettings,
  DefaultClusterFactory,
  PowerOfTwoBufferPool
}

import com.mongodb.management.JMXConnectionPoolListener

/**
 * A factory for creating a [[MongoClientProvider MongoClient]] instances.
 *
 * All that is needed is a `RequiredTypesAndTransformersProvider` implementation to be mixed in:
 *
 * @example {{{
 *   object MongoClient extends MongoClientCompanion with RequiredTypesAndTransformers
 * }}}
 *
 */
trait MongoClientCompanion {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * If you use a `case class` implementation of [[MongoClientProvider]] the this method is
   * automatically provided, otherwise you will need to implement.
   *
   * @return MongoClient
   */
  def apply(options: MongoClientOptions, cluster: Cluster, operationOptions: OperationOptions): Client

  /**
   * Create a default MongoClient at localhost:27017
   *
   * @return MongoClient
   */
  def apply(): Client = this(new ServerAddress())

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param uri The [[org.mongodb.scala.core.MongoClientURI MongoClientURI]] connection string
   * @return MongoClient
   */
  def apply(uri: String): Client = this(MongoClientURI(uri))

  /**
   * Create a MongoClient instance from a ServerAddress instance
   * @param serverAddress a representation of the location of a MongoDB server - i.e. server name and port number
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress): Client = {
    val options = MongoClientOptions()
    this(serverAddress, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress instance
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, options: MongoClientOptions): Client = {
    val credentialList = List[MongoCredential]()
    this(serverAddress, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress and Credentials
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, credentialList: List[MongoCredential]): Client = {
    val options = MongoClientOptions()
    this(serverAddress, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress, Credentials and MongoClientOptions
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, credentialList: List[MongoCredential], options: MongoClientOptions): Client = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    this(serverAddress, credentialList, options, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a ServerAddress, Credentials, MongoClientOptions and BufferProvider
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @param bufferProvider The Buffer Provider to use for the MongoClient
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress,
            credentialList: List[MongoCredential], options: MongoClientOptions, bufferProvider: BufferProvider): Client = {
    val clusterSettings = options.requiredReplicaSetName match {
      case Some(name) =>
        ClusterSettings.builder()
          .mode(ClusterConnectionMode.SINGLE)
          .hosts(List[ServerAddress](serverAddress).asJava)
          .requiredReplicaSetName(name)
          .build()
      case None =>
        ClusterSettings.builder()
          .mode(ClusterConnectionMode.SINGLE)
          .hosts(List[ServerAddress](serverAddress).asJava)
          .build()
    }

    val cluster = getDefaultCluster(clusterSettings, credentialList, options, bufferProvider)
    this(options, cluster, defaultOptions)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses
   *
   * @param seedList A list of ServerAddresses to connect to
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress]): Client = {
    val options = MongoClientOptions()
    this(seedList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential]): Client = {
    val options = MongoClientOptions()
    this(seedList, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], options: MongoClientOptions): Client = {
    val credentialList = List[MongoCredential]()
    this(seedList, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential], options: MongoClientOptions): Client = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    this(seedList, credentialList, options, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @param bufferProvider The Buffer Provider to use for the MongoClient
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential], options: MongoClientOptions,
            bufferProvider: BufferProvider): Client = {
    // scalastyle:off null
    val replicaSetName = options.requiredReplicaSetName match {
      case None       => null
      case Some(name) => name
    }
    // scalastyle:on null
    val clusterSettings = ClusterSettings.builder()
      .hosts(seedList.asJava)
      .requiredReplicaSetName(replicaSetName)
      .build()

    val cluster = getDefaultCluster(clusterSettings, credentialList, options, bufferProvider)
    this(options, cluster, defaultOptions)
  }

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param mongoClientURI the MongoClientURI
   * @return MongoClient
   */
  def apply(mongoClientURI: MongoClientURI): Client = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    this(mongoClientURI, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param mongoClientURI the MongoClientURI
   * @param bufferProvider The Buffer Provider to use for the MongoClient
   * @return MongoClient
   */
  def apply(mongoClientURI: MongoClientURI, bufferProvider: BufferProvider): Client = {
    val options = mongoClientURI.options
    val credentialList = mongoClientURI.mongoCredentials
    mongoClientURI.hosts.size match {
      case 1 =>
        val clusterSettings = ClusterSettings.builder()
          .mode(ClusterConnectionMode.SINGLE)
          .hosts(List[ServerAddress](new ServerAddress(mongoClientURI.hosts(0))).asJava)
        options.requiredReplicaSetName collect {
          case replicaSet: String => clusterSettings.requiredReplicaSetName(replicaSet)
        }
        val cluster = getDefaultCluster(clusterSettings.build, credentialList, options, bufferProvider)
        this(options, cluster, defaultOptions)
      case _ =>
        val seedList: List[ServerAddress] = mongoClientURI.hosts.map(hostName => new ServerAddress(hostName))
        this(seedList, credentialList, options, bufferProvider)
    }
  }

  private val DEFAULT_CODEC_REGISTRY: RootCodecRegistry = new RootCodecRegistry(List(new ValueCodecProvider,
    new DocumentCodecProvider,
    new BsonValueCodecProvider)
    .asJava)

  private val defaultOptions = OperationOptions.builder()
    .codecRegistry(DEFAULT_CODEC_REGISTRY)
    .readPreference(primary)
    .writeConcern(WriteConcern.ACKNOWLEDGED).build()

  private def getDefaultCluster(clusterSettings: ClusterSettings,
                                credentialList: List[MongoCredential],
                                options: MongoClientOptions,
                                bufferProvider: BufferProvider): Cluster = {

    val streamFactory = new AsynchronousSocketChannelStreamFactory(options.socketSettings, options.sslSettings)
    val heartbeatStreamFactory = new AsynchronousSocketChannelStreamFactory(options.heartbeatSocketSettings, options.sslSettings)
    val connectionPoolListener = new JMXConnectionPoolListener()

    // scalastyle:off null
    new DefaultClusterFactory().create(
      clusterSettings,
      options.serverSettings,
      options.connectionPoolSettings,
      streamFactory,
      heartbeatStreamFactory,
      credentialList.asJava,
      null,
      connectionPoolListener,
      null)
    // scalastyle:on null
  }

}
