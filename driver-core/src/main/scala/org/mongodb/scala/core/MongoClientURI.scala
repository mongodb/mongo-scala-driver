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

import java.net.URLDecoder

import scala.collection.JavaConverters._

import com.mongodb._
import com.mongodb.AuthenticationMechanism.{ GSSAPI, MONGODB_CR, MONGODB_X509, PLAIN }
import com.mongodb.diagnostics.logging.Logger
import com.mongodb.diagnostics.logging.Loggers

// scalastyle:off cyclomatic.complexity method.length

/**
 * Represents a [[http://docs.mongodb.org/manual/reference/connection-string/ connection string (URI)]]
 * which can be used to create a MongoClient instance. The URI describes the hosts to
 * be used and options.
 *
 * == The format of the URI is: ==
 *
 * {{{ mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]] }}}
 *
 *  - '''`mongodb://`''' is a required prefix to identify that this is a string in the standard connection format.
 *  - '''`username:password@`''' are optional.  If given, the driver will attempt to login to a database after
 *     connecting to a database server.  For some authentication mechanisms, only the username is specified and the password is not,
 *     in which case the ":" after the username is left off as well
 *  - '''`host1`''' is the only required part of the URI.  It identifies a server address to connect to.
 *  - '''`:portX`''' is optional and defaults to :27017 if not provided.
 *  - '''`/database`''' is the name of the database to login to and thus is only relevant if the
 *  - '''`username:password@`''' syntax is used. If not specified the "admin" database will be used by default.
 *  - '''`?options`''' are connection options. Note that if `database` is absent there is still a `/`
 *     required between the last host and the `?options` introducing the options. Options are name=value pairs and the pairs
 *     are separated by "&amp;". For backwards compatibility, ";" is accepted as a separator in addition to "&amp;",
 *     but should be considered as deprecated.
 *
 * The following options are supported (case insensitive):
 *
 * == Replica set configuration ==
 *
 *  - '''`replicaSet=name`''': Implies that the hosts given are a seed list, and the driver will attempt to find
 *    all members of the set.
 *
 * == Connection Configuration ==
 *
 *  - '''`ssl=true|false`''': Whether to connect using SSL.
 *  - '''`connectTimeoutMS=ms`''': How long a connection can take to be opened before timing out.
 *  - '''`socketTimeoutMS=ms`''': How long a send or receive on a socket can take before timing out.
 *  - '''`maxIdleTimeMS=ms`''': Maximum idle time of a pooled connection. A connection that exceeds this limit will be closed
 *  - '''`maxLifeTimeMS=ms`''': Maximum life time of a pooled connection. A connection that exceeds this limit will be closed
 *
 * == Connection pool configuration ==
 *
 *  - '''`maxPoolSize=n`''': The maximum number of connections in the connection pool.
 *  - '''`waitQueueMultiple=n`''': this multiplier, multiplied with the maxPoolSize setting, gives the maximum number of
 *     threads that may be waiting for a connection to become available from the pool.  All further threads will get an
 *     exception right away.
 *  - '''`waitQueueTimeoutMS=ms`''': The maximum wait time in milliseconds that a thread may wait for a connection to
 *    become available.
 *
 * == Write concern configuration ==
 *
 *  - '''`safe=true|false`'''
 *    - `true`: the driver sends a getLastError command after every update to ensure that the update succeeded
 *                   (see also `w` and `wtimeoutMS`).
 *    - `false`: the driver does not send a getLastError command after every update.
 *  - '''`w=wValue`'''
 *    - The driver adds { w : wValue } to the getLastError command. Implies `safe=true`.
 *    - wValue is typically a number, but can be any string in order to allow for specifications like `"majority"`
 *  - '''`wtimeoutMS=ms`'''
 *    - The driver adds { wtimeout : ms } to the getlasterror command. Implies `safe=true`.
 *    - Used in combination with `w`
 *
 * == Read preference configuration ==
 *
 *  - '''`slaveOk=true|false`''': Whether a driver connected to a replica set will send reads to slaves/secondaries.
 *  - '''`readPreference=enum`''': The read preference for this connection.  If set, it overrides any slaveOk value.
 *    - Enumerated values:
 *     - `primary`
 *     - `primaryPreferred`
 *     - `secondary`
 *     - `secondaryPreferred`
 *     - `nearest`
 *  - '''`readPreferenceTags=string`'''.  A representation of a tag set as a comma-separated list of colon-separated
 *     key-value pairs, e.g. `dc:ny,rack:1`.  Spaces are stripped from beginning and end of all keys and values.
 *     To specify a list of tag sets, using multiple readPreferenceTags,
 *     e.g. `readPreferenceTags=dc:ny,rack:1;readPreferenceTags=dc:ny;readPreferenceTags=`
 *     - Note the empty value for the last one, which means match any secondary as a last resort.
 *     - Order matters when using multiple readPreferenceTags.
 *
 * == Authentication configuration ==
 *
 *  - '''`authMechanism=MONGO-CR|GSSAPI|PLAIN|MONGODB-X509`''': The authentication mechanism to use if a credential was supplied.
 *    The default is MONGODB-CR, which is the native MongoDB Challenge Response mechanism.  For the GSSAPI and MONGODB-X509 mechanisms,
 *    no password is accepted, only the username.
 *  - '''`authSource=string`''': The source of the authentication credentials.  This is typically the database that
 *    the credentials have been created.  The value defaults to the database specified in the path portion of the URI.
 *    If the database is specified in neither place, the default value is "admin".  This option is only respected when using the MONGO-CR
 *    mechanism (the default).
 *  - '''`gssapiServiceName=string`''': This option only applies to the GSSAPI mechanism and is used to alter the service name.
 *
 * @see [[org.mongodb.scala.core.MongoClientOptions]] for the default values for all options
 */
object MongoClientURI {

  private final val LOGGER: Logger = Loggers.getLogger("uri")

  /**
   * Creates a MongoClientURI from the given string.
   *
   * @param uri the URI
   */
  def apply(uri: String): MongoClientURI = MongoClientURI(uri, MongoClientOptions())

  /**
   * Creates a MongoClientURI from the given URI string, and MongoClientOptions.
   * The mongoClientOptions can be configured with default options, which may be overridden by options specified in
   * the URI string.
   *
   * @param uri     the URI
   * @param mongoClientOptions the default options to use for the MongoClient
   * @see [[org.mongodb.scala.core.MongoClientOptions]]
   * @since 0.1
   */
  def apply(uri: String, mongoClientOptions: MongoClientOptions): MongoClientURI = {

    val connectionString: ConnectionString = new ConnectionString(uri)

    // Get the username & password from the authentication part of the uri
    val hosts = connectionString.getHosts.asScala.toList
    val database = Option(connectionString.getDatabase)
    val collection = Option(connectionString.getCollection)
    val empty = List.empty[MongoCredential]
    val mongoCredentials = connectionString.getCredentialList.asScala.toList
    val mergedMongoClientOptions: MongoClientOptions = createOptions(connectionString, mongoClientOptions)

    new MongoClientURI(uri, hosts, database, collection, mongoCredentials, mergedMongoClientOptions)
  }

  /**
   * Creates MongoClientOptions from the options in the uri
   *
   * @param connectionString the converted options from the uri
   * @param mongoClientOptions the MongoClientOptions passed along with the uri
   * @return MongoClientOptions
   */
  private def createOptions(connectionString: ConnectionString, mongoClientOptions: MongoClientOptions): MongoClientOptions = {

    val UriMaxConnectionPoolSize: Int = Option(connectionString.getMaxConnectionPoolSize) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.maxConnectionPoolSize
    }

    val UriMinConnectionPoolSize = Option(connectionString.getMinConnectionPoolSize) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.minConnectionPoolSize
    }

    val UriMaxConnectionIdleTime = Option(connectionString.getMaxConnectionIdleTime) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.maxConnectionIdleTime
    }

    val UriMaxConnectionLifeTime = Option(connectionString.getMaxConnectionLifeTime) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.maxConnectionLifeTime
    }

    val UriThreadsAllowedToBlockForConnectionMultiplier = Option(connectionString.getThreadsAllowedToBlockForConnectionMultiplier) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.threadsAllowedToBlockForConnectionMultiplier
    }

    val UriMaxWaitTime = Option(connectionString.getMaxWaitTime) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.maxWaitTime
    }

    val UriConnectTimeout = Option(connectionString.getConnectTimeout) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.connectTimeout
    }

    val UriSocketTimeout = Option(connectionString.getSocketTimeout) match {
      case Some(value) => value.intValue
      case None        => mongoClientOptions.socketTimeout
    }

    val UriSsLEnabled = Option(connectionString.getSslEnabled) match {
      case Some(value) => value.booleanValue()
      case None        => mongoClientOptions.SslEnabled
    }

    val UriRequiredReplicaSetName = Option(connectionString.getRequiredReplicaSetName) match {
      case Some(value) => Some(value)
      case None        => mongoClientOptions.requiredReplicaSetName
    }

    val UriWriteConcern = Option(connectionString.getWriteConcern) match {
      case Some(value) => value
      case None        => mongoClientOptions.writeConcern
    }

    val UriReadPreference = Option(connectionString.getReadPreference) match {
      case Some(value) => value
      case None        => mongoClientOptions.readPreference
    }

    mongoClientOptions.copy(maxConnectionPoolSize = UriMaxConnectionPoolSize,
      minConnectionPoolSize = UriMinConnectionPoolSize,
      maxConnectionIdleTime = UriMaxConnectionIdleTime,
      maxConnectionLifeTime = UriMaxConnectionLifeTime,
      threadsAllowedToBlockForConnectionMultiplier = UriThreadsAllowedToBlockForConnectionMultiplier,
      maxWaitTime = UriMaxWaitTime,
      connectTimeout = UriConnectTimeout,
      socketTimeout = UriSocketTimeout,
      SslEnabled = UriSsLEnabled,
      requiredReplicaSetName = UriRequiredReplicaSetName,
      writeConcern = UriWriteConcern,
      readPreference = UriReadPreference)
  }

}

/**
 * Represents a [[http://docs.mongodb.org/manual/reference/connection-string/ connection string (URI)]]
 * which can be used to create a MongoClient instance. The URI describes the hosts to
 * be used and options.
 *
 * @note You will normally only use the factory function
 *
 * @param uri The connection string
 * @param hosts A list of hosts to connect to
 * @param database Optional database name
 * @param collection Optional collection name
 * @param mongoCredentials Optional authentication credentials
 * @param options Optional extra configuration options.
 */
case class MongoClientURI(uri: String, hosts: List[String], database: Option[String], collection: Option[String],
                          mongoCredentials: List[MongoCredential], options: MongoClientOptions) {

  /**
   * The connection string (URI) used to build this MongoClientURI
   * @return uri
   */
  override def toString: String = uri
}

// scalastyle:on cyclomatic.complexity method.length
