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

import rx.lang.scala.Observable

import org.mongodb.Document

import org.mongodb.scala.core.admin.MongoDatabaseAdminProvider
import org.mongodb.scala.rxscala.{CommandResponseHandler, MongoDatabase, RequiredTypes}

/**
 * The MongoDatabaseAdmin
 *
 * @param database the MongoDatabase being administrated
 */
case class MongoDatabaseAdmin(database: MongoDatabase) extends MongoDatabaseAdminProvider
  with CommandResponseHandler with RequiredTypes {

  /**
   * A helper function that takes the collection name documents and returns a list of names from those documents
   *
   * @return the collection names
   */
  protected def collectionNamesHelper: Observable[Document] => Observable[String] = { result =>
    result map (doc => doc.getString("name") match {
      case dollarCollectionName: String if dollarCollectionName.contains("$") => ""
      case collectionName: String => collectionName.substring(database.name.length() + 1)
    }) filter (s => s.length > 0)
  }

  /**
   * A type transformer that takes a `Observable[Void]` and converts it to `Observable[Unit]`
   * @return Observable[Unit]
   */
  protected def voidToUnitConverter: Observable[Void] => Observable[Unit] = result => result map { v => Unit }
}
