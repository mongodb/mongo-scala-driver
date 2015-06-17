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

package org.mongodb.scala.unit

import java.lang.reflect.Modifier.isStatic

import scala.collection.JavaConverters._

import org.mongodb.scala.{ ReadPreference, Tag, TagSet }
import org.scalatest.{ FlatSpec, Matchers }

class ReadPreferenceSpec extends FlatSpec with Matchers {

  "ReadPreference" should "have the same methods as the wrapped ReadPreference" in {
    val wrapped = classOf[com.mongodb.ReadPreference].getDeclaredMethods.filter(f => isStatic(f.getModifiers)).map(_.getName).toSet
    val local = ReadPreference.getClass.getDeclaredMethods.map(_.getName).toSet

    local should equal(wrapped)
  }

  it should "return the correct primary ReadPreferences" in {
    val readPreference = ReadPreference.primary()
    readPreference shouldBe com.mongodb.ReadPreference.primary()
  }

  it should "return the correct primaryPreferred ReadPreferences" in {
    val readPreference = ReadPreference.primaryPreferred()
    readPreference shouldBe com.mongodb.ReadPreference.primaryPreferred()

    val readPreference1 = ReadPreference.primaryPreferred(TagSet())
    readPreference1 shouldBe com.mongodb.ReadPreference.primaryPreferred(TagSet())

    val readPreference2 = ReadPreference.primaryPreferred(TagSet(Tag("name", "value")))
    readPreference2 shouldBe com.mongodb.ReadPreference.primaryPreferred(TagSet(Tag("name", "value")))

    val readPreference3 = ReadPreference.primaryPreferred(List(TagSet(List(Tag("name", "value")))))
    readPreference3 shouldBe com.mongodb.ReadPreference.primaryPreferred(List(TagSet(List(Tag("name", "value")))).asJava)
  }

  it should "return the correct secondary based ReadPreferences" in {
    val readPreference = ReadPreference.secondary()
    readPreference shouldBe com.mongodb.ReadPreference.secondary()

    val readPreference1 = ReadPreference.secondary(TagSet())
    readPreference1 shouldBe com.mongodb.ReadPreference.secondary(TagSet())

    val readPreference2 = ReadPreference.secondary(TagSet(Tag("name", "value")))
    readPreference2 shouldBe com.mongodb.ReadPreference.secondary(TagSet(Tag("name", "value")))

    val readPreference3 = ReadPreference.secondary(List(TagSet(List(Tag("name", "value")))))
    readPreference3 shouldBe com.mongodb.ReadPreference.secondary(List(TagSet(List(Tag("name", "value")))).asJava)
  }

  it should "return the correct secondaryPreferred based ReadPreferences" in {
    val readPreference = ReadPreference.secondaryPreferred()
    readPreference shouldBe com.mongodb.ReadPreference.secondaryPreferred()

    val readPreference1 = ReadPreference.secondaryPreferred(TagSet())
    readPreference1 shouldBe com.mongodb.ReadPreference.secondaryPreferred(TagSet())

    val readPreference2 = ReadPreference.secondaryPreferred(TagSet(Tag("name", "value")))
    readPreference2 shouldBe com.mongodb.ReadPreference.secondaryPreferred(TagSet(Tag("name", "value")))

    val readPreference3 = ReadPreference.secondaryPreferred(List(TagSet(List(Tag("name", "value")))))
    readPreference3 shouldBe com.mongodb.ReadPreference.secondaryPreferred(List(TagSet(List(Tag("name", "value")))).asJava)
  }

  it should "return the correct nearest based ReadPreferences" in {
    val readPreference = ReadPreference.nearest()
    readPreference shouldBe com.mongodb.ReadPreference.nearest()

    val readPreference2 = ReadPreference.nearest(TagSet(Tag("name", "value")))
    readPreference2 shouldBe com.mongodb.ReadPreference.nearest(TagSet(Tag("name", "value")))

    val readPreference3 = ReadPreference.nearest(List(TagSet(List(Tag("name", "value")))))
    readPreference3 shouldBe com.mongodb.ReadPreference.nearest(List(TagSet(List(Tag("name", "value")))).asJava)
  }

  it should "return the correct ReadPreference for valueOf" in {
    val readPreference = ReadPreference.valueOf("Primary")
    readPreference shouldBe com.mongodb.ReadPreference.primary()

    val readPreference2 = ReadPreference.valueOf("PrimaryPreferred", List(TagSet(Tag("name", "value"))))
    readPreference2 shouldBe com.mongodb.ReadPreference.primaryPreferred(List(TagSet(Tag("name", "value"))).asJava)
  }

}
