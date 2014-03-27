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
package org.mongodb.scala

import rx.lang.scala.Subject
import rx.lang.scala.subjects.ReplaySubject

import org.mongodb.{AsyncBlock => JAsyncBlock}

/**
 * An AsyncBlock that uses a `Subject` to add new documents into.
 *
 * Currently using a `ReplaySubject` as a PublishSubject could start emitting items
 * before a subscription has taken hold.
 *
 * TODO: Investigate a buffered ReplaySubject once released (https://github.com/Netflix/RxJava/issues/865)
 *
 * @tparam T the type of document
 */
case class AsyncBlock[T](subject: Subject[T] = ReplaySubject[T]()) extends JAsyncBlock[T] {

  def done(): Unit = subject.onCompleted()

  def apply(document: T): Unit = subject.onNext(document)

}
