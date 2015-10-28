/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala

import org.bson.codecs.configuration.CodecRegistries._

import org.mongodb.scala.bson.codecs.{ DEFAULT_CODEC_REGISTRY, DocumentCodecProvider }
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class MongoClientSettingsSpec extends FlatSpec with Matchers with MockFactory {

  "MongoClientSettings" should "default with the Scala Codec Registry" in {
    MongoClientSettings.builder().build().getCodecRegistry should equal(DEFAULT_CODEC_REGISTRY)
  }

  it should "keep the default Scala Codec Registry in no codec registry is set" in {
    val settings = MongoClientSettings.builder().readPreference(ReadPreference.nearest()).build()
    MongoClientSettings.builder(settings).build().getCodecRegistry should equal(DEFAULT_CODEC_REGISTRY)
  }

  it should "use a none default Codec Registry if set" in {
    val codecRegistry = fromProviders(DocumentCodecProvider())
    val settings = MongoClientSettings.builder().codecRegistry(codecRegistry).build()
    MongoClientSettings.builder(settings).build().getCodecRegistry should equal(codecRegistry)
  }

}
