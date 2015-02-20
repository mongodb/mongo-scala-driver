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

package com.mongodb.scala.reactivestreams.client.codecs

import com.mongodb.scala.reactivestreams.client.collection.immutable.{ Document => ImmutableDocument }
import com.mongodb.scala.reactivestreams.client.collection.mutable.{ Document => MutableDocument }
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{ CodecProvider, CodecRegistry }

/**
 * A {@code CodecProvider} for the Document class and all the default Codec implementations on which it depends.
 *
 */
case class DocumentCodecProvider() extends CodecProvider {

  val IMMUTABLE: Class[ImmutableDocument] = classOf[ImmutableDocument]
  val MUTABLE: Class[MutableDocument] = classOf[MutableDocument]

  // scalastyle:off null
  @SuppressWarnings(Array("unchecked"))
  def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
    clazz match {
      case IMMUTABLE => ImmutableDocumentCodec(registry).asInstanceOf[Codec[T]]
      case MUTABLE   => MutableDocumentCodec(registry).asInstanceOf[Codec[T]]
      case _         => null
    }
  }
  // scalastyle:on null
}
