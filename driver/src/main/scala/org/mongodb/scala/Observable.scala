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

import org.mongodb.scala.internal._

/**
 * A companion object to the Java [[Observable]]
 */
object Observable {

  /**
   * Creates an Observable from an Iterable.
   *
   * Convenient for testing and or debugging.
   *
   * @param from the iterable to create the observable from
   * @tparam A the type of Iterable
   * @return an Observable that emits each item from the Iterable
   */
  def apply[A](from: Iterable[A]): Observable[A] = IterableObservable[A](from)

}

