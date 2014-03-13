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
 * [Project URL - TODO]
 *
 */
package org.mongodb.scala

import java.io.Closeable
import java.util.concurrent._

import scala.collection.JavaConverters._

import org.mongodb.MongoCredential
import org.mongodb.connection._
import org.mongodb.session.ClusterSession

import org.mongodb.scala.admin.MongoClientAdmin
import org.mongodb.scala.connection.GetDefaultCluster

/**
 * A factory for creating a [[org.mongodb.MongoClient MongoClient]] instance.
 */
object MongoClient extends GetDefaultCluster {

  /**
   * Create a default MongoClient at localhost:27017
   *
   * @return MongoClient
   */
  def apply(): MongoClient = MongoClient(new ServerAddress())

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param uri The [[org.mongodb.scala.MongoClientURI$ MongoClientURI]] connection string
   * @return MongoClient
   */
  def apply(uri: String): MongoClient = MongoClient(MongoClientURI(uri))

  /**
   * Create a MongoClient instance from a ServerAddress instance
   * @param serverAddress a representation of the location of a MongoDB server - i.e. server name and port number
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress): MongoClient = {
    val options = MongoClientOptions()
    MongoClient(serverAddress, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress instance
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, options: MongoClientOptions): MongoClient = {
    val credentialList = List[MongoCredential]()
    MongoClient(serverAddress, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress and Credentials
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, credentialList: List[MongoCredential]): MongoClient = {
    val options = MongoClientOptions()
    MongoClient(serverAddress, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a ServerAddress, Credentials and MongoClientOptions
   *
   * @param serverAddress the representation of the location of a MongoDB server
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(serverAddress: ServerAddress, credentialList: List[MongoCredential], options: MongoClientOptions): MongoClient = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    MongoClient(serverAddress, credentialList, options, bufferProvider)
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
  def apply(serverAddress: ServerAddress, credentialList: List[MongoCredential], options: MongoClientOptions, bufferProvider: BufferProvider): MongoClient = {
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
    MongoClient(options, cluster, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses
   *
   * @param seedList A list of ServerAddresses to connect to
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress]): MongoClient = {
    val options = MongoClientOptions()
    MongoClient(seedList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential]): MongoClient = {
    val options = MongoClientOptions()
    MongoClient(seedList, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], options: MongoClientOptions): MongoClient = {
    val credentialList = List[MongoCredential]()
    MongoClient(seedList, credentialList, options)
  }

  /**
   * Create a MongoClient instance from a List of ServerAddresses and MongoCredentials
   *
   * @param seedList A list of ServerAddresses to connect to
   * @param credentialList the credentials to authenticate to a MongoDB server
   * @param options MongoClientOptions to use for the MongoClient
   * @return MongoClient
   */
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential], options: MongoClientOptions): MongoClient = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    MongoClient(seedList, credentialList, options, bufferProvider)
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
  def apply(seedList: List[ServerAddress], credentialList: List[MongoCredential], options: MongoClientOptions, bufferProvider: BufferProvider): MongoClient = {
    // scalastyle:off null
    val replicaSetName = options.requiredReplicaSetName match {
      case None => null
      case Some(name) => name
    }
    // scalastyle:on null
    val clusterSettings = ClusterSettings.builder()
      .hosts(seedList.asJava)
      .requiredReplicaSetName(replicaSetName)
      .build()

    val cluster = getDefaultCluster(clusterSettings, credentialList, options, bufferProvider)
    MongoClient(options, cluster, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param mongoClientURI the MongoClientURI
   * @return MongoClient
   */
  def apply(mongoClientURI: MongoClientURI): MongoClient = {
    val bufferProvider: BufferProvider = new PowerOfTwoBufferPool()
    MongoClient(mongoClientURI, bufferProvider)
  }

  /**
   * Create a MongoClient instance from a connection string
   *
   * @param mongoClientURI the MongoClientURI
   * @param bufferProvider The Buffer Provider to use for the MongoClient
   * @return MongoClient
   */
  def apply(mongoClientURI: MongoClientURI, bufferProvider: BufferProvider): MongoClient = {
    val options = mongoClientURI.options
    val credentialList: List[MongoCredential] = mongoClientURI.mongoCredentials match {
      case Some(credential) => List(credential)
      case None => List.empty[MongoCredential]
    }
    mongoClientURI.hosts.size match {
      case 1 =>
        val clusterSettings = ClusterSettings.builder()
          .mode(ClusterConnectionMode.SINGLE)
          .hosts(List[ServerAddress](new ServerAddress(mongoClientURI.hosts(0))).asJava)
        options.requiredReplicaSetName collect {
          case replicaSet: String => clusterSettings.requiredReplicaSetName(replicaSet)
        }
        val cluster = getDefaultCluster(clusterSettings.build, credentialList, options, bufferProvider)
        MongoClient(options, cluster, bufferProvider)
      case _ =>
        val seedList: List[ServerAddress] = mongoClientURI.hosts.map(hostName => new ServerAddress(hostName))
        MongoClient(seedList, credentialList, options, bufferProvider)
    }
  }
}

/**
 * The MongoClient
 *
 * Normally created via the [[org.mongodb.scala.MongoClient$ MongoClient]] companion object helpers
 *
 * @param options The connection options
 * @param cluster The underlying cluster
 * @param bufferProvider The buffer provider to use
 */
case class MongoClient(options: MongoClientOptions, cluster: Cluster, bufferProvider: BufferProvider) extends Closeable {

  /**
   * Helper to get a database
   *
   * @param databaseName the name of the database
   * @return MongoDatabase
   */
  def apply(databaseName: String): MongoDatabase = database(databaseName)

  /**
   * Helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def apply(databaseName: String, databaseOptions: MongoDatabaseOptions): MongoDatabase =
    database(databaseName, databaseOptions)

  /**
   * An explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @return MongoDatabase
   */
  def database(databaseName: String): MongoDatabase = database(databaseName, MongoDatabaseOptions(options))

  /**
   * an explicit helper to get a database
   *
   * @param databaseName the name of the database
   * @param databaseOptions the options to use with the database
   * @return MongoDatabase
   */
  def database(databaseName: String, databaseOptions: MongoDatabaseOptions): MongoDatabase =
    MongoDatabase(databaseName, this, databaseOptions)

  /**
   * Close the MongoClient and its connections
   */
  def close() {
    cluster.close()
    executorService.shutdownNow()
  }

  /**
   * The MongoClientAdmin which provides admin methods
   */
  val admin: MongoClientAdmin = MongoClientAdmin(this)

  private[scala] def session: ClusterSession = new ClusterSession(cluster, executorService)

  private val executorService: ExecutorService = Executors.newCachedThreadPool()
}
