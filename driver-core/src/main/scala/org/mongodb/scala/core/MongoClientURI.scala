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

import org.mongodb.{AuthenticationMechanism, MongoCredential, ReadPreference, WriteConcern}
import org.mongodb.AuthenticationMechanism.{GSSAPI, MONGODB_CR, MONGODB_X509, PLAIN}
import org.mongodb.connection.Tags
import org.mongodb.diagnostics.Loggers
import org.mongodb.diagnostics.logging.Logger

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

  private final val PREFIX: String = "mongodb://"
  private final val UTF_8: String = "UTF-8"
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

    // Split the URI into components
    val unprefixedURI: String = uri.substring(PREFIX.length)
    val authParamsPos: Int = unprefixedURI.indexOf("@")
    val lastSlashPos: Int = unprefixedURI.lastIndexOf("/")
    val optionsPos: Int = unprefixedURI.indexOf("?")

    // Initial validation
    if (!uri.startsWith(PREFIX)) {
      throw new IllegalArgumentException(s"uri needs to start with $PREFIX")
    }

    if (lastSlashPos == -1 && optionsPos >= 0) {
      throw new IllegalArgumentException("URI contains options without trailing slash")
    }

    // Get the authentication information from the uri
    val authPart: Option[String] = authParamsPos match {
      case idx: Int if idx >= 0 => Some(unprefixedURI.substring(0, idx))
      case _ => None
    }
    // Get the server information from the uri
    val serverPart: Option[String] = lastSlashPos match {
      case idx: Int if idx >= 0 => Some(unprefixedURI.substring(authParamsPos + 1, idx))
      case _ => Some(unprefixedURI.substring(authParamsPos + 1))
    }
    // Get the name space (database / collection name) information from the uri
    val nsPart: Option[String] = optionsPos match {
      case idx: Int if idx >= 0 => Some(unprefixedURI.substring(lastSlashPos + 1, idx))
      case none: Int if lastSlashPos >= 0 => Some(unprefixedURI.substring(lastSlashPos + 1))
      case _ => None
    }
    // Get the options information from the uri
    val optionsPart: Option[String] = optionsPos match {
      case idx: Int if idx >= 0 => Some(unprefixedURI.substring(optionsPos + 1))
      case _ => None
    }

    // Get the username & password from the authentication part of the uri
    val (username, password) = authPart match {
      case Some(auth) =>
        auth.split(":") match {
          case Array(s1) => (Some(URLDecoder.decode(s1, UTF_8)), None)
          case Array(s1, s2@_*) => (Some(URLDecoder.decode(s1, UTF_8)), Some(URLDecoder.decode(s2.mkString(":"), UTF_8)))
        }
      case _ => (None, None)
    }

    // Get the hosts list from the server part of the uri
    val hosts: List[String] = serverPart match {
      case Some(servers) => servers.split(",").toList
      case None => List.empty[String]
    }

    // Get the database & collection from the namespace part of the uri
    val (database, collection) = nsPart match {
      case Some(nameSpace) =>
        nameSpace.split('.') match {
          case Array(s1) => (Some(s1), None)
          case Array(s1, s2@_*) => (Some(s1), Some(s2.mkString(".")))
        }
      case _ => (None, None)
    }

    // Convert the options part into an map
    val optionsMap: Map[String, List[String]] = optionsPart match {
      case Some(options) => options.split("&|;").toList.map({
        str =>
          str.split('=') match {
            case Array(s1) => (s1.toLowerCase, "")
            case Array(s1, s2) => (s1.toLowerCase, s2)
            case _ => throw new IllegalArgumentException(s"Bad uri options: $str")
          }
      }).groupBy(_._1).map({
        case (k, v) => (k, v.map(_._2))
      })
      case _ => Map[String, List[String]]()
    }

    // Get MongoClientOptions and MongoCredentials
    val mergedMongoClientOptions: MongoClientOptions = createOptions(optionsMap, mongoClientOptions)
    val mongoCredentials: Option[MongoCredential] = username match {
      case Some(user) => Some(createCredentials(optionsMap, database, user, password.getOrElse("").toCharArray))
      case None => None
    }

    // Warn the user if they have used any unsupported keys in the uri
    warnAboutUnsupportedKeys(uri, optionsMap)

    new MongoClientURI(uri, hosts, database, collection, mongoCredentials, mergedMongoClientOptions)
  }

  /**
   * Creates MongoClientOptions from the options in the uri
   *
   * @param optionsMap the converted options from the uri
   * @param mongoClientOptions the MongoClientOptions passed along with the uri
   * @return MongoClientOptions
   */
  private def createOptions(optionsMap: Map[String, List[String]], mongoClientOptions: MongoClientOptions): MongoClientOptions = {

    val maxConnectionPoolSize = optionsMap.get("maxpoolsize") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.maxConnectionPoolSize
    }

    val minConnectionPoolSize = optionsMap.get("minpoolsize") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.minConnectionPoolSize
    }

    val maxConnectionIdleTime = optionsMap.get("maxidletimems") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.maxConnectionIdleTime
    }

    val maxConnectionLifeTime = optionsMap.get("maxlifetimems") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.maxConnectionLifeTime
    }

    val threadsAllowedToBlockForConnectionMultiplier = optionsMap.get("waitqueuemultiple") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.threadsAllowedToBlockForConnectionMultiplier
    }

    val maxWaitTime = optionsMap.get("waitqueuetimeoutms") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.maxWaitTime
    }

    val connectTimeout = optionsMap.get("connecttimeoutms") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.connectTimeout
    }

    val socketTimeout = optionsMap.get("sockettimeoutms") match {
      case Some(value) => value.reverse.head.toInt
      case None => mongoClientOptions.socketTimeout
    }

    val autoConnectRetry = optionsMap.get("autoconnectretry") match {
      case Some(value) => value.reverse.head.toBoolean
      case None => mongoClientOptions.autoConnectRetry
    }

    val SSLEnabled = optionsMap.get("ssl") match {
      case Some(value) => value.reverse.head.toBoolean
      case None => mongoClientOptions.SSLEnabled
    }

    val requiredReplicaSetName = optionsMap.get("replicaset") match {
      case Some(value) => Some(value.reverse.head.toString)
      case None => mongoClientOptions.requiredReplicaSetName
    }

    val writeConcern = createWriteConcern(optionsMap) match {
      case Some(value) => value
      case None => mongoClientOptions.writeConcern
    }

    val readPreference = createReadPreference(optionsMap) match {
      case Some(value) => value
      case None => mongoClientOptions.readPreference
    }

    mongoClientOptions.copy(maxConnectionPoolSize=maxConnectionPoolSize,
                            minConnectionPoolSize=minConnectionPoolSize,
                            maxConnectionIdleTime=maxConnectionIdleTime,
                            maxConnectionLifeTime=maxConnectionLifeTime,
                            threadsAllowedToBlockForConnectionMultiplier=threadsAllowedToBlockForConnectionMultiplier,
                            maxWaitTime=maxWaitTime,
                            connectTimeout=connectTimeout,
                            socketTimeout=socketTimeout,
                            autoConnectRetry=autoConnectRetry,
                            SSLEnabled=SSLEnabled,
                            requiredReplicaSetName=requiredReplicaSetName,
                            writeConcern=writeConcern,
                            readPreference=readPreference)
  }

  /**
   * Create a WriteConcern from the options in the uri
   * @param optionsMap the converted options map
   * @return Some(WriteConcern) or None
   */
  private def createWriteConcern(optionsMap: Map[String, List[String]]): Option[WriteConcern] = {

    val safe: Option[Boolean] = optionsMap.get("safe") match {
      case Some(v) => Some(v.reverse.head.toBoolean)
      case _ => None
    }

    val w: Option[String] = optionsMap.get("w") match {
      case Some(v) => Some(v.reverse.head)
      case _ => None
    }

    val wTimeout: Int = optionsMap.get("wtimeout") match {
      case Some(v) => v.reverse.head.toInt
      case _ => 0
    }

    val fsync: Boolean = optionsMap.get("fsync") match {
      case Some(v) => v.reverse.head.toBoolean
      case _ => false
    }

    val journal: Boolean = optionsMap.get("j") match {
      case Some(v) => v.reverse.head.toBoolean
      case _ => false
    }

    // Determine if the user has supplied any custom write concerns
    // * Custom write concerns imply safe = true
    // * Otherwise checks if safe was explicitly set.
    val hasCustomWriteConcern: Boolean = w != None || wTimeout != 0 || fsync || journal
    hasCustomWriteConcern match {
      case true => w match {
        case Some(wInt) if wInt.matches("[+-]?\\d+") => Some(new WriteConcern(wInt.toInt, wTimeout, fsync, journal))
        case Some(wString) => Some(new WriteConcern(wString, wTimeout, fsync, journal))
        case _ => Some(new WriteConcern(1, wTimeout, fsync, journal))

      }
      case false => safe match {
        case Some(true) => Some(WriteConcern.ACKNOWLEDGED)
        case Some(false) => Some(WriteConcern.UNACKNOWLEDGED)
        case _ => None
      }
    }
  }

  /**
   * Creates a ReadPreference from the options provided in the uri
   * @param optionsMap the converted options map
   * @return Some(ReadPreference) or None
   */
  private def createReadPreference(optionsMap: Map[String, List[String]]): Option[ReadPreference] = {

    val slaveOk: Option[Boolean] = optionsMap.get("slaveok") match {
      case Some(v) => Some(v.reverse.head.toBoolean)
      case _ => None
    }

    val readPreferenceType: Option[String] = optionsMap.get("readpreference") match {
      case Some(v) => Some(v.reverse.head)
      case _ => None
    }

    // Get the tag set from the URI
    // tags can be a comma-separated list of colon-separated key-value pairs and can
    // even be a list of tags. Note order matters when using multiple readPreferenceTags.
    // For example:
    // readPreferenceTags=dc:ny,rack:1&readPreferenceTags=dc:ny&readPreferenceTags=
    val tagsList: List[Tags] = optionsMap.get("readpreferencetags") match {
      case Some(rawTagList) =>
        rawTagList.map({
          rawTagString: String => {
            val tags = new Tags()
            rawTagString.split(",").foreach({
              str: String =>
                str.split(":") match {
                  case Array(s1) => Unit
                  case Array(s1, s2) => tags append(s1.trim(), s2.trim())
                  case _ => throw new IllegalArgumentException(s"Bad read preference tags: $rawTagString")
                }
            })
            tags
          }
        })
      case _ => List.empty[Tags]
    }

    readPreferenceType match {
      case Some(name) => Some(ReadPreference.valueOf(name, tagsList.asJava))
      case _ => slaveOk match {
        case Some(true) => Some(ReadPreference.secondaryPreferred)
        case _ => None
      }
    }
  }

  /**
   * Create Credentials from information provided in the uri
   *
   * @param optionsMap the converted options map
   * @param database the database if present from the uri
   * @param username the username if present from the uri
   * @param password the password if present from the uri
   * @return a MongoCredential
   */
  private def createCredentials(optionsMap: Map[String, List[String]], database: Option[String],
                                username: String, password: Array[Char]): MongoCredential = {

    val mechanism = optionsMap.get("authmechanism") match {
      case Some(names) => AuthenticationMechanism.fromMechanismName(names.reverse.head)
      case None => MONGODB_CR
    }

    val authSource: String = optionsMap.get("authsource") match {
      case Some(name) => name.reverse.head
      case None => database getOrElse "admin"
    }

    mechanism match {
      case PLAIN => MongoCredential.createPlainCredential(username, authSource, password)
      case MONGODB_CR => MongoCredential.createMongoCRCredential(username, authSource, password)
      case MONGODB_X509 => MongoCredential.createMongoX509Credential(username)
      case GSSAPI =>
        val gssapiCredential: MongoCredential = MongoCredential.createGSSAPICredential(username)
        optionsMap.get("gssapiservicename") collect {
          case names => gssapiCredential.withMechanismProperty("SERVICE_NAME", names.reverse.head)
        }
        gssapiCredential
      case _ => throw new UnsupportedOperationException(s"Unsupported authentication mechanism in the URI: $mechanism")
    }
  }

  /**
   * Warn the user if they used an unsupported key
   * @param uri the intial uri
   * @param optionsMap the converted options map
   */
  private def warnAboutUnsupportedKeys(uri: String, optionsMap: Map[String, List[String]]) {
    val supportedKeys = Set("minpoolsize", "maxpoolsize", "waitqueuemultiple", "waitqueuetimeoutms", "connecttimeoutms",
      "maxidletimems", "maxlifetimems", "sockettimeoutms", "sockettimeoutms", "autoconnectretry",
      "ssl", "replicaset", "slaveok", "readpreference", "readpreferencetags", "safe", "w",
      "wtimeout", "fsync", "j", "authmechanism", "authsource", "gssapiservicename")

    for (key <- optionsMap.keys.toSet[String] -- supportedKeys) LOGGER.warn(s"Unsupported option '$key' on URI '$uri'.")
  }
}

/**
 * Represents a [[http://docs.mongodb.org/manual/reference/connection-string/ connection string (URI)]]
 * which can be used to create a MongoClient instance. The URI describes the hosts to
 * be used and options. See: [[org.mongodb.scala.core.MongoClientURI$ MongoClientURI]]
 *
 * @note You will normally only use the factory function: [[org.mongodb.scala.core.MongoClientURI$ MongoClientURI]]
 *
 * @param uri The connection string
 * @param hosts A list of hosts to connect to
 * @param database Optional database name
 * @param collection Optional collection name
 * @param mongoCredentials Optional authentication credentials
 * @param options Optional extra configuration options.
 */
case class MongoClientURI(uri: String, hosts: List[String], database: Option[String], collection: Option[String],
                          mongoCredentials: Option[MongoCredential], options: MongoClientOptions) {

  /**
   * The connection string (URI) used to build this MongoClientURI
   * @return uri
   */
  override def toString: String = uri
}

// scalastyle:on cyclomatic.complexity method.length
