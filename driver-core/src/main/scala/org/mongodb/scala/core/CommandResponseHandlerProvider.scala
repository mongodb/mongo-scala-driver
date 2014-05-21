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

import org.mongodb.CommandResult

/**
 * A special handler for command responses that ignores certain error conditions.
 *
 * In certain scenarios we may want to suppress an exception where the result of the operation is the same. For example:
 * `MongoCollection.admin.dropIndexes()` doesn't throw an exception if the collection doesn't exist.
 *
 * Implementations of this trait must implement the `handleNamedErrors`
 *
 */
trait CommandResponseHandlerProvider {

  this: RequiredTypesProvider =>

  /**
   * Gracefully handles any commands that might throw a "ns not found" error
   *
   * @param commandFuture the Future[CommandResult] to wrap
   * @return a fixed Future[CommandResult]
   */
  protected[scala] def handleNameSpaceErrors(commandFuture: ResultType[CommandResult]) =
    handleNamedErrors(commandFuture, Seq("ns not found"))

  /**
   * Handles any command results correctly.
   *
   * @param commandFuture the Future[CommandResult] to wrap
   * @return
   */
  protected[scala] def handleErrors(commandFuture: ResultType[CommandResult]): ResultType[CommandResult] =
    handleNamedErrors(commandFuture, Seq.empty[String])

  /**
   * Sometimes we need to handle certain errors gracefully, without cause to throw an exception.
   *
   * This must be implemented by the implementer of the trait and should ignore any `CommandResult` errors
   * if they include any of the `namedErrors`.
   *
   *@param commandFuture the ResultType[CommandResult] to wrap
   * @param namedErrors A sequence of errors that have the same end result as a successful operation
   *                    eg: `collection.admin.dropIndexes()` when a collection doesn't exist
   * @return a fixed ResultType[CommandResult]
   */
  protected[scala] def handleNamedErrors(commandFuture: ResultType[CommandResult],
                                         namedErrors: Seq[String]): ResultType[CommandResult]

}
