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

import java.util.concurrent.TimeUnit.MILLISECONDS

import com.mongodb.{ ReadPreference, WriteConcern }
import com.mongodb.connection.{ ConnectionPoolSettings, SSLSettings, ServerSettings, SocketSettings }

// scalastyle:off magic.number

/**
 * Various settings to control the behavior of a `MongoClient`
 *
 * @param description the description for this MongoClient, which is used in various places like logging.
 * @param readPreference the read preference to use for queries, map-reduce, aggregation, and count.
 *                       Default `ReadPreference.Primary()`
 * @param writeConcern The write concern to use. Default is `WriteConcern.ACKNOWLEDGED`.
 * @param minConnectionPoolSize The minimum number of connections per server for this MongoClient instance.
 *                              Those connections will be kept in a pool when idle, and
 *                              the pool will ensure over time that it contains at least this minimum number.
 *                              Default `0`
 * @param maxConnectionPoolSize The maximum number of connections allowed per host for this MongoClient instance.
 *                              Those connections will be kept in a pool when idle. Once the pool is exhausted,
 *                              any operation requiring a connection will block waiting for an available
 *                              connection. Default is `100`.
 * @param threadsAllowedToBlockForConnectionMultiplier this multiplier, multiplied with the maxConnectionPoolSize
 *                                                     setting, gives the maximum number of threads that may be waiting
 *                                                     for a connection to become available from the pool. All further
 *                                                     threads will get an exception right away. For example if
 *                                                     `maxConnectionPoolSize` is `10` and
 *                                                     `threadsAllowedToBlockForConnectionMultiplier` is `5`, then up
 *                                                     to `50` threads can wait for a connection.  Default is `5`.
 * @param maxWaitTime The maximum wait time in milliseconds that a thread may wait for a connection to become
 *                    available. Default is `120,000`. A value of `0` means that it will not wait.
 *                    A negative value means to wait indefinitely.
 * @param maxConnectionIdleTime The maximum idle time of a pooled connection.  A zero value indicates no limit to the
 *                              idle time.  A pooled connection that has exceeded its idle time will be closed and
 *                              replaced when necessary by a new connection.
 * @param maxConnectionLifeTime The maximum life time of a pooled connection.  A zero value indicates no limit to the
 *                              life time.  A pooled connection that has exceeded its life time will be closed and
 *                              replaced when necessary by a new connection.
 * @param connectTimeout The connection timeout in milliseconds.  A value of `0` means no timeout. It is used solely
 *                       when establishing a new connection `java.net.Socket#connect(java.net.SocketAddress, int)`.
 *                       Default is 10,000.
 * @param socketTimeout The socket timeout in milliseconds. It is used for I/O socket read and write operations
 *                      `java.net.Socket#setSoTimeout(int)`. Default is `0` and means no timeout.
 * @param socketKeepAlive This flag controls the socket keep alive feature that keeps a connection alive through
 *                        firewalls. `java.net.Socket#setKeepAlive(boolean)`. Default is `false`.
 * @param maxAutoConnectRetryTime The maximum amount of time in MS to spend retrying to open connection to the same
 *                                server. Default is `0`, which means to use the default `15s` if autoConnectRetry is on.
 * @param SslEnabled Whether to use SSL. The default is `false`.
 * @param heartbeatFrequency This is the frequency that the driver will attempt to determine the current state of each
 *                           server in the cluster. The default value is `5000` milliseconds.
 * @param minHeartbeatFrequency This is the minimum heartbeat frequency.  In the event that the driver has to frequently re-check a server's availability, it will
 *                              wait at least this long since the previous check to avoid wasted effort. The default value is `10` milliseconds.
 * @param heartbeatConnectTimeout the connect timeout for connections used for the cluster heartbeat.  The default value is `20,000` milliseconds.
 * @param heartbeatSocketTimeout  the socket timeout for connections used for the cluster heartbeat.  The default value is `20,000` milliseconds.
 * @param requiredReplicaSetName the required replica set name.  With this option set, the MongoClient instance will
 *                               1. Connect in replica set mode, and discover all members of the set based on the given servers
 *                               2. Make sure that the set name reported by all members matches the required set name.
 *                               3. Refuse to service any requests if any member of the seed list is not part of a replica set with the required name.
 */
case class MongoClientOptions(description: String = "",
                              readPreference: ReadPreference = ReadPreference.primary,
                              writeConcern: WriteConcern = WriteConcern.ACKNOWLEDGED,
                              minConnectionPoolSize: Int = 0,
                              maxConnectionPoolSize: Int = 100,
                              threadsAllowedToBlockForConnectionMultiplier: Int = 5,
                              maxWaitTime: Int = 1000 * 60 * 2,
                              maxConnectionIdleTime: Int = 0,
                              maxConnectionLifeTime: Int = 0,
                              connectTimeout: Int = 1000 * 10,
                              socketTimeout: Int = 0,
                              socketKeepAlive: Boolean = false,
                              maxAutoConnectRetryTime: Long = 0,
                              SslEnabled: Boolean = false,
                              heartbeatFrequency: Int = 5000,
                              minHeartbeatFrequency: Int = 10,
                              heartbeatConnectTimeout: Int = 20000,
                              heartbeatSocketTimeout: Int = 20000,
                              requiredReplicaSetName: Option[String] = None) {

  val socketSettings = SocketSettings.builder
    .connectTimeout(connectTimeout, MILLISECONDS)
    .readTimeout(socketTimeout, MILLISECONDS)
    .keepAlive(socketKeepAlive)
    .build()
  val heartbeatSocketSettings = SocketSettings.builder
    .connectTimeout(heartbeatConnectTimeout, MILLISECONDS)
    .readTimeout(heartbeatSocketTimeout, MILLISECONDS)
    .keepAlive(socketKeepAlive)
    .build()
  val connectionPoolSettings = ConnectionPoolSettings.builder
    .minSize(minConnectionPoolSize)
    .maxSize(maxConnectionPoolSize)
    .maxWaitQueueSize(maxConnectionPoolSize * threadsAllowedToBlockForConnectionMultiplier)
    .maxWaitTime(maxWaitTime, MILLISECONDS)
    .maxConnectionIdleTime(maxConnectionIdleTime, MILLISECONDS)
    .maxConnectionLifeTime(maxConnectionLifeTime, MILLISECONDS)
    .build()
  val serverSettings = ServerSettings.builder
    .heartbeatFrequency(heartbeatFrequency, MILLISECONDS)
    .minHeartbeatFrequency(minHeartbeatFrequency, MILLISECONDS)
    .build()
  val sslSettings = SSLSettings.builder
    .enabled(SslEnabled)
    .build()

  override lazy val toString: String = {
    s"""MongoClientOptions(description="$description",
                            | minConnectionPoolSize="$minConnectionPoolSize",
                            | maxConnectionPoolSize="$maxConnectionPoolSize",
                            | threadsAllowedToBlockForConnectionMultiplier="$threadsAllowedToBlockForConnectionMultiplier",
                            | maxWaitTime="$maxWaitTime",
                            | maxConnectionIdleTime="$maxConnectionIdleTime",
                            | maxConnectionLifeTime="$maxConnectionLifeTime",
                            | connectTimeout="$connectTimeout",
                            | socketTimeout="$socketTimeout",
                            | socketKeepAlive="$socketKeepAlive",
                            | maxAutoConnectRetryTime="$maxAutoConnectRetryTime",
                            | readPreference="$readPreference",
                            | writeConcern="$writeConcern",
                            | SslEnabled="$SslEnabled",
                            | heartbeatFrequency="$heartbeatFrequency",
                            | minHeartbeatFrequency="$minHeartbeatFrequency",
                            | heartbeatConnectTimeout="$heartbeatConnectTimeout",
                            | heartbeatSocketTimeout="$heartbeatSocketTimeout",
                            | requiredReplicaSetName="$requiredReplicaSetName")""".stripMargin.replaceAll("\n", "")
  }
}

// scalastyle:on magic.number
