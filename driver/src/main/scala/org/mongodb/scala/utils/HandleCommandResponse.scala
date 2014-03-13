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
 *
 */
package org.mongodb.scala.utils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import org.mongodb.{CommandResult, MongoCommandFailureException}

/**
 * A special handler for command responses, to ensure the future result is a success or failure.
 *
 * In certain scenarios we may want to suppress an exception where the result of the operation is the same. For example:
 * {{MongoCollection.admin.dropIndexes()}} doesn't throw an exception if the collection doesn't exist.
 */
trait HandleCommandResponse {

  /**
   * Gracefully handles any commands that might throw a "ns not found" error
   *
   * @param commandFuture the Future[CommandResult] to wrap
   * @return a fixed Future[CommandResult]
   */
  protected def handleNameSpaceErrors(commandFuture: Future[CommandResult]) =
    handleNamedErrors(commandFuture, Seq("ns not found"))

  /**
   * Handles any command results correctly.
   *
   * @param commandFuture the Future[CommandResult] to wrap
   * @return
   */
  protected def handleErrors(commandFuture: Future[CommandResult]): Future[CommandResult] =
    handleNamedErrors(commandFuture, Seq.empty[String])

  /**
   * Sometimes we need to handle certain errors gracefully, without cause to throw an exception.
   *
   * @param commandFuture the Future[CommandResult] to wrap
   * @param namedErrors A sequence of errors that have the same end result as a successful operation
   *                    eg: `collection.admin.dropIndexes()` when a collection doesn't exist
   * @param executor the implicit context
   * @return a fixed Future[CommandResult]
   */
  protected def handleNamedErrors(commandFuture: Future[CommandResult],
                                  namedErrors: Seq[String])(implicit executor: ExecutionContext): Future[CommandResult] = {
    val promise = Promise[CommandResult]()
    commandFuture onComplete {
      case Success(result) =>
          result.getErrorMessage match {
          case error: String if !namedErrors.contains(error) =>
            promise failure new MongoCommandFailureException(result)
          case _ => promise success result
        }
      case Failure(e: MongoCommandFailureException) =>
          e.getCommandResult.getErrorMessage match {
          case error: String if namedErrors.contains(error) => promise success e.getCommandResult
          case _ => promise failure e
        }
      case Failure(other) => promise failure other
    }
    promise.future
  }

}
