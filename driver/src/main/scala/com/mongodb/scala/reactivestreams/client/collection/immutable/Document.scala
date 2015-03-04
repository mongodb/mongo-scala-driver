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

package com.mongodb.scala.reactivestreams.client.collection.immutable

import com.mongodb.scala.reactivestreams.client.collection
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{ BsonDocument, BsonValue }

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{ Builder, ListBuffer }
import scala.collection.{ Traversable, TraversableLike }

/**
 * The immutable [[Document]] companion object for easy creation.
 */
object Document {

  /**
   * Create a new empty Document
   * @return a new Document
   */
  def empty: Document = apply()

  /**
   * Create a new Document
   * @return a new Document
   */
  def apply(): Document = Document(new BsonDocument())

  /**
   * Create a new document from the elems
   * @param elems   the key/value pairs that make up the Document
   * @return        a new Document consisting key/value pairs given by `elems`.
   */
  def apply[B](elems: (String, B)*)(implicit ev: B => BsonValue): Document = apply(elems)

  /**
   * Create a new document from the Map
   * @param iterable the key/value pairs that make up the Document
   * @return         a new Document consisting key/value pairs given by `elems`.
   */
  def apply[B](iterable: Iterable[(String, B)])(implicit ev: B => BsonValue): Document = {
    val underlying = new BsonDocument()
    for ((k, v) <- iterable) underlying.put(k, v)
    apply(underlying)
  }

  /**
   * A implicit builder factory.
   *
   * @return a builder factory.
   */
  implicit def canBuildFrom: CanBuildFrom[Traversable[(String, BsonValue)], (String, BsonValue), Document] = {
    new CanBuildFrom[Traversable[(String, BsonValue)], (String, BsonValue), Document] {
      def apply(): Builder[(String, BsonValue), Document] = builder
      def apply(from: Traversable[(String, BsonValue)]): Builder[(String, BsonValue), Document] = builder
    }
  }

  private def builder = ListBuffer[(String, BsonValue)]() mapResult fromSeq

  private def fromSeq(ts: Seq[(String, BsonValue)]): Document = {
    val underlying = new BsonDocument()
    ts.foreach(kv => underlying.put(kv._1, kv._2))
    apply(underlying)
  }
}

/**
 * An immutable Document implementation.
 *
 * A strictly typed `Map[String, BsonValue]` like structure that traverses the elements in insertion order. Unlike native scala maps there
 * is no variance in the value type and it always has to be a `BsonValue`.  The [[com.mongodb.scala.reactivestreams.client.Implicits]]
 * helper provides simple interactions with Documents taking native data types and converting them to `BsonValues`.
 *
 * @note All user operations on the document are immutable. The *only* time the document can mutate state is when an `_id` is added by
 *       the underlying [[com.mongodb.scala.reactivestreams.client.codecs.ImmutableDocumentCodec]] codec on insertion to the database.
 * @param underlying the underlying BsonDocument which stores the data.
 */
case class Document(protected[client] val underlying: BsonDocument)
    extends collection.BaseDocument[Document] with TraversableLike[(String, BsonValue), Document] {

  /**
   * Creates a new immutable document
   * @param underlying the underlying BsonDocument
   * @return a new document
   */
  protected[client] def apply(underlying: BsonDocument) = new Document(underlying)

  /**
   * Applies a function `f` to all elements of this document.
   *
   * @param  f   the function that is applied for its side-effect to every element.
   *             The result of function `f` is discarded.
   *
   * @tparam  U  the type parameter describing the result of function `f`.
   *             This result will always be ignored. Typically `U` is `Unit`,
   *             but this is not necessary.
   *
   * @usecase def foreach(f: A => Unit): Unit
   * @inheritdoc
   */
  override def foreach[U](f: ((String, BsonValue)) => U): Unit = underlying.asScala foreach f

  /**
   * Creates a new builder for this collection type.
   */
  override def newBuilder: Builder[(String, BsonValue), Document] = Document.builder

}
