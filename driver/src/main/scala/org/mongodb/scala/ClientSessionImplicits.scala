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

import com.mongodb.async.SingleResultCallback
import org.mongodb.scala.internal.ObservableHelper.observeCompleted

/**
 * Extends the Java [[ClientSession]] and adds helpers for committing and aborting transactions.
 *
 * Automatically imported into the `org.mongodb.scala` namespace
 */
trait ClientSessionImplicits {

  /**
   * The implicit ClientSession with Scala helpers
   * @param clientSession the clientSession
   */
  implicit class ScalaClientSession(clientSession: ClientSession) {

    /**
     * Commit a transaction in the context of this session.
     *
     * A transaction can only be commmited if one has first been started.
     */
    def commitTransaction(): SingleObservable[Completed] = observeCompleted(clientSession.commitTransaction(_: SingleResultCallback[Void]))

    /**
     * Abort a transaction in the context of this session.
     *
     * A transaction can only be aborted if one has first been started.
     */
    def abortTransaction(): SingleObservable[Completed] = observeCompleted(clientSession.abortTransaction(_: SingleResultCallback[Void]))
  }
}
