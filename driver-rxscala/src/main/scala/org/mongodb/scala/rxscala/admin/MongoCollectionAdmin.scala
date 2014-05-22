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
package org.mongodb.scala.rxscala.admin

import java.util

import scala.collection.JavaConverters._

import rx.lang.scala.Observable

import org.mongodb.{CommandResult, Document}

import org.mongodb.scala.core.admin.MongoCollectionAdminProvider
import org.mongodb.scala.rxscala.{CommandResponseHandler, MongoCollection, RequiredTypes}

case class MongoCollectionAdmin[T](collection: MongoCollection[T]) extends MongoCollectionAdminProvider[T]
  with CommandResponseHandler with RequiredTypes {

  /**
   * A type transformer that takes a `Observable[Void]` and converts it to `Observable[Unit]`
   *
   * @return ResultType[Unit]
   */
  protected def voidToUnitConverter: Observable[Void] => Observable[Unit] = result => result map {v => Unit}

  /**
   * A helper that gets the `capped` field from the statistics document
   *
   * @return
   */
  protected def getCappedFromStatistics: Observable[Document] => Observable[Boolean] = {
    result => result map { doc => doc.get("capped").asInstanceOf[Boolean] }
  }

  /**
   * A helper that takes a `Observable[CommandResult]` and picks out the `CommandResult.getResponse()` to return the
   * response Document as `Observable[Document]`.
   *
   * @return Observable[Document]
   */
  protected def getResponseHelper: Observable[CommandResult] => Observable[Document] = {
        result => result map { cmdResult => cmdResult.getResponse }
  }


  /**
   * A type transformer that takes a `Observable[util.List[Document]]` and converts it to `Observable[Document]`
   * @return  Observable[Document]
   */
  protected def javaListToListResultTypeConverter: Observable[util.List[Document]] => Observable[Document] = {
    result => { result.map({ docs => Observable.from(docs.asScala.toList) }).concat }
  }
}
