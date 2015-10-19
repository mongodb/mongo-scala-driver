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

package org.mongodb.scala.internal

import scala.concurrent.duration.Duration

import com.mongodb.{ WriteConcern => JWriteConcern }

import org.mongodb.scala.WriteConcern

private[scala] trait WriteConcernImplicits {

  implicit class ScalaWriteConcern[T](jWriteConcern: JWriteConcern) {

    /**
     * Constructs a new WriteConcern from the current one and the specified journal value
     *
     * @param journal true if journalling is required for acknowledgement, false if not, or null if unspecified
     * @return the new WriteConcern
     */
    def withJournal(journal: Boolean): WriteConcern = jWriteConcern.withJ(journal)

    /**
     * Constructs a new WriteConcern from the current one and the specified wTimeout in the given time unit.
     *
     * @param wTimeout the wTimeout, which must be &gt;= 0 and &lt;= Integer.MAX_VALUE after conversion to milliseconds
     * @return the WriteConcern with the given wTimeout
     */
    def withWTimeout(wTimeout: Duration): WriteConcern = {
      jWriteConcern.getWObject.isInstanceOf[String] match {
        case true  => new JWriteConcern(jWriteConcern.getWString, wTimeout.toMillis.toInt, jWriteConcern.getFsync, jWriteConcern.getJ)
        case false => new JWriteConcern(jWriteConcern.getW, wTimeout.toMillis.toInt, jWriteConcern.getFsync, jWriteConcern.getJ)
      }
    }

  }

}
