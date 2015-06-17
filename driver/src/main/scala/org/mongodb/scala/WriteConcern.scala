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

import com.mongodb.{ WriteConcern => JWriteConcern }

/**
 * Controls the acknowledgment of write operations with various options.
 *
 * ==`w`==
 * - 0: Don't wait for acknowledgement from the server
 * - 1: Wait for acknowledgement, but don't wait for secondaries to replicate
 * - >=2: Wait for one or more secondaries to also acknowledge
 *
 * ==`wtimeout` - how long to wait for slaves before failing ==
 * - 0: indefinite
 * - >0: time to wait in milliseconds
 *
 * ==Other options:==
 *
 * - `j`: If true block until write operations have been committed to the journal. Cannot be used in combination with
 *    `fsync`. Prior to MongoDB 2.6 this option was ignored if the server was running without journaling.
 *    Starting with MongoDB 2.6 write operations will fail with an exception if this option is used when the server
 *    is running without journaling.
 * - `fsync`: If true and the server is running without journaling, blocks until the server has synced all data files to disk.
 *    If the server is running with journaling, this acts the same as the `j` option, blocking until write operations have been
 *    committed to the journal. Cannot be used in combination with `j`. In almost all cases the `j` flag should be used in
 *    preference to this one.
 *
 * @since 1.0
 */
object WriteConcern {
  /**
   * Write operations that use this write concern will wait for acknowledgement from the primary server before returning. Exceptions are
   * raised for network issues, and server errors.
   */
  val ACKNOWLEDGED: JWriteConcern = JWriteConcern.ACKNOWLEDGED
  /**
   * Write operations that use this write concern will return as soon as the message is written to the socket. Exceptions are raised for
   * network issues, but not server errors.
   */
  val UNACKNOWLEDGED: JWriteConcern = JWriteConcern.UNACKNOWLEDGED
  /**
   * Exceptions are raised for network issues, and server errors; the write operation waits for the server to flush the data to disk.
   */
  val FSYNCED: JWriteConcern = JWriteConcern.FSYNCED
  /**
   * Exceptions are raised for network issues, and server errors; the write operation waits for the server to group commit to the journal
   * file on disk.
   */
  val JOURNALED: JWriteConcern = JWriteConcern.JOURNALED
  /**
   * Exceptions are raised for network issues, and server errors; waits for at least 2 servers for the write operation.
   */
  val REPLICA_ACKNOWLEDGED: JWriteConcern = JWriteConcern.REPLICA_ACKNOWLEDGED

  /**
   * Exceptions are raised for network issues, and server errors; waits on a majority of servers for the write operation.
   */
  val MAJORITY: JWriteConcern = JWriteConcern.MAJORITY

  /**
   * Default constructor keeping all options as default.  Be careful using this constructor, as it's equivalent to
   * `WriteConcern.UNACKNOWLEDGED`, so writes may be lost without any errors being reported.
   *
   * @see WriteConcern#UNACKNOWLEDGED
   */
  def apply(): JWriteConcern = new JWriteConcern()

  /**
   * Calls `WriteConcern(w: Int, wtimeout: Int, fsync: Boolean)` with wtimeout=0 and fsync=false
   *
   * @param w number of writes
   */
  def apply(w: Int): JWriteConcern = new JWriteConcern(w)

  /**
   * Tag based Write Concern with wtimeout=0, fsync=false, and j=false
   *
   * @param w Write Concern tag
   */
  def apply(w: String): JWriteConcern = new JWriteConcern(w)

  /**
   * Calls ``WriteConcern(w: Int, wtimeout: Int, fsync: Boolean)` with fsync=false
   *
   * @param w        number of writes
   * @param wtimeout timeout for write operation
   */
  def apply(w: Int, wtimeout: Int): JWriteConcern = new JWriteConcern(w, wtimeout)

  /**
   * Calls `WriteConcern(w: Int, wtimeout: Int, fsync: Boolean)` with w=1 and wtimeout=0
   *
   * @param fsync whether or not to fsync
   */
  def apply(fsync: Boolean): JWriteConcern = new JWriteConcern(fsync)

  /**
   * Creates a WriteConcern object.
   *
   * Specifies the number of servers to wait for on the write operation, and exception raising behavior
   *
   *  `w` represents the number of servers:
   *
   * - `w=-1` None, no checking is done
   * - `w=0` None, network socket errors raised
   * - `w=1` Checks server for errors as well as network socket errors raised
   * - `w>1` Checks servers (w) for errors as well as network socket errors raised
   *
   *
   * @param w        number of writes
   * @param wtimeout timeout for write operation
   * @param fsync    whether or not to fsync
   */
  def apply(w: Int, wtimeout: Int, fsync: Boolean): JWriteConcern = new JWriteConcern(w, wtimeout, fsync)

  /**
   * Creates a WriteConcern object.
   *
   * Specifies the number of servers to wait for on the write operation, and exception raising behavior
   *
   *  `w` represents the number of servers:
   *
   * - `w=-1` None, no checking is done
   * - `w=0` None, network socket errors raised
   * - `w=1` Checks server for errors as well as network socket errors raised
   * - `w>1` Checks servers (w) for errors as well as network socket errors raised
   *
   *
   * @param w        number of writes
   * @param wtimeout timeout for write operation
   * @param fsync    whether or not to fsync
   * @param j        whether writes should wait for a journaling group commit
   */
  def apply(w: Int, wtimeout: Int, fsync: Boolean, j: Boolean): JWriteConcern = new JWriteConcern(w, wtimeout, fsync, j)

  /**
   * Creates a WriteConcern object.
   *
   * Specifies the number of servers to wait for on the write operation, and exception raising behavior
   *
   *  `w` represents the number of servers:
   *
   * - `w=-1` None, no checking is done
   * - `w=0` None, network socket errors raised
   * - `w=1` Checks server for errors as well as network socket errors raised
   * - `w>1` Checks servers (w) for errors as well as network socket errors raised
   *
   *
   * @param w        number of writes
   * @param wtimeout timeout for write operation
   * @param fsync    whether or not to fsync
   * @param j        whether writes should wait for a journaling group commit
   */
  def apply(w: String, wtimeout: Int, fsync: Boolean, j: Boolean): JWriteConcern = new JWriteConcern(w, wtimeout, fsync, j)

  /**
   * Create a Majority Write Concern that requires a majority of servers to acknowledge the write.
   *
   * @param wtimeout timeout for write operation
   * @param fsync    whether or not to fsync
   * @param j        whether writes should wait for a journal group commit
   * @return Majority, a subclass of WriteConcern that represents the write concern requiring most servers to acknowledge the write
   */
  def majorityWriteConcern(wtimeout: Int, fsync: Boolean, j: Boolean): JWriteConcern.Majority =
    JWriteConcern.majorityWriteConcern(wtimeout, fsync, j)

  /**
   * A write concern that blocks acknowledgement of a write operation until a majority of replica set members have applied it.
   */
  object Majority {
    /**
     * Create a new Majority WriteConcern.
     */
    def apply(): JWriteConcern.Majority = new JWriteConcern.Majority()

    /**
     * Create a new WriteConcern with the given configuration.
     *
     * @param wtimeout timeout for write operation
     * @param fsync    whether or not to fsync
     * @param j        whether writes should wait for a journaling group commit
     */
    def apply(wtimeout: Int, fsync: Boolean, j: Boolean): JWriteConcern.Majority = new JWriteConcern.Majority(wtimeout, fsync, j)
  }

}
