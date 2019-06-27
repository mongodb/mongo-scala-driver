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

package org.mongodb.scala.bson

import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
import org.bson.codecs.configuration.CodecRegistry
import com.mongodb.async.client.MongoClients

package object codecs {
  val DEFAULT_CODEC_REGISTRY: CodecRegistry = fromRegistries(
    fromProviders(DocumentCodecProvider(), IterableCodecProvider()),
    MongoClients.getDefaultCodecRegistry
  )

  /**
   * Type alias to the `BsonTypeClassMap`
   */
  type BsonTypeClassMap = org.bson.codecs.BsonTypeClassMap

  /**
   * Companion to return the default `BsonTypeClassMap`
   */
  object BsonTypeClassMap {
    def apply(): BsonTypeClassMap = new BsonTypeClassMap()
  }

  /**
   * Type alias to the `BsonTypeCodecMap`
   */
  type BsonTypeCodecMap = org.bson.codecs.BsonTypeCodecMap

}
