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

import com.mongodb.scala.reactivestreams.client.collection.mutable.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.{ BsonDocumentCodec, CollectibleCodec, DecoderContext, EncoderContext }
import org.bson.{ BsonReader, BsonValue, BsonWriter }

/**
 * Companion helper for mutable Document instances.
 */
object MutableDocumentCodec {
  def apply(): MutableDocumentCodec = MutableDocumentCodec(None)
  def apply(registry: CodecRegistry): MutableDocumentCodec = MutableDocumentCodec(Some(registry))
}

/**
 * A Codec for mutable Document instances.
 */
case class MutableDocumentCodec(registry: Option[CodecRegistry]) extends CollectibleCodec[Document] {

  lazy val underlying: BsonDocumentCodec = {
    if (registry.isDefined) {
      new BsonDocumentCodec(registry.get)
    } else {
      new BsonDocumentCodec()
    }
  }

  override def generateIdIfAbsentFromDocument(document: Document): Unit =
    underlying.generateIdIfAbsentFromDocument(document.underlying)

  override def documentHasId(document: Document): Boolean = underlying.documentHasId(document.underlying)

  override def getDocumentId(document: Document): BsonValue = underlying.getDocumentId(document.underlying)

  override def encode(writer: BsonWriter, value: Document, encoderContext: EncoderContext): Unit =
    underlying.encode(writer, value.underlying, encoderContext)

  override def getEncoderClass: Class[Document] = classOf[Document]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): Document =
    Document(underlying.decode(reader, decoderContext))
}
