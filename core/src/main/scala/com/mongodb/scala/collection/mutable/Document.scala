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

package com.mongodb.scala.collection.mutable

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{Builder, ListBuffer}
import scala.collection.{Traversable, TraversableLike, TraversableOnce}

import org.bson.{BsonDocument, BsonValue}
import com.mongodb.scala.collection.BaseDocument


/**
 * Mutable [[Document]] companion object for easy creation.
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

  private def builder: Builder[(String, BsonValue), Document] = ListBuffer[(String, BsonValue)]() mapResult fromSeq

  private def fromSeq(ts: Seq[(String, BsonValue)]): Document = {
    val underlying = new BsonDocument()
    ts.foreach(kv => underlying.put(kv._1, kv._2))
    apply(underlying)
  }
}

/**
 * An mutable Document implementation.
 *
 * A strictly typed `Map[String, BsonValue]` like structure that traverses the elements in insertion order. Unlike native scala maps there
 * is no variance in the value type and it always has to be a `BsonValue`.  The [[com.mongodb.scala.Implicits]]
 * helper provides simple interactions with Documents taking native data types and converting them to `BsonValues`.
 *
 * @param underlying the underlying BsonDocument which stores the data.
 */
case class Document(protected[scala] val underlying: BsonDocument)
    extends BaseDocument[Document] with TraversableLike[(String, BsonValue), Document] {

  /**
   * Creates a new immutable document
   * @param underlying the underlying BsonDocument
   * @return a new document
   */
  protected[scala] def apply(underlying: BsonDocument) = new Document(underlying)

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

  // scalastyle:off method.name
  /**
   *  Adds a new key/value pair to this document.
   *  If the document already contains a mapping for the key, it will be overridden by the new value.
   *
   *  @param    kv the key/value pair.
   *  @return   the document itself
   */
  def +=[B](kv: (String, B))(implicit ev: B => BsonValue): Document = {
    underlying.put(kv._1, kv._2)
    this
  }

  /**
   * ${Add}s two or more elements to this document.
   *
   *  @param elem1 the first element to $add.
   *  @param elem2 the second element to $add.
   *  @param elems the remaining elements to $add.
   *  @return the $coll itself
   */
  def +=[B](elem1: (String, B), elem2: (String, B), elems: (String, B)*)(implicit ev: B => BsonValue): Document = this += elem1 += elem2 ++= elems

  /**
   * ${Add}s all elements produced by a TraversableOnce to this document.
   *
   *  @param xs   the TraversableOnce producing the elements to $add.
   *  @return  the document itself.
   */
  def ++=[B](xs: TraversableOnce[(String, B)])(implicit ev: B => BsonValue): Document = { xs foreach (this += _); this }
  // scalastyle:on method.name

  /**
   * Adds a new key/value pair to this map.
   *  If the document already contains a mapping for the key, it will be overridden by the new value.
   *
   *  @param key    The key to update
   *  @param value  The new value
   */
  def update[B](key: String, value: B)(implicit ev: B => BsonValue) { this += ((key, value)) }

  /**
   *  Adds a new key/value pair to this document and optionally returns previously bound value.
   *  If the document already contains a mapping for the key, it will be overridden by the new value.
   *
   * @param key    the key to update
   * @param value  the new value
   * @return an option value containing the value associated with the key before the `put` operation was executed, or
   *         `None` if `key` was not defined in the document before.
   */
  def put[B](key: String, value: B)(implicit ev: B => BsonValue): Option[BsonValue] = {
    val r = get(key)
    update(key, value)
    r
  }

  /**
   * If given key is already in this document, returns associated value.
   *
   *  Otherwise, computes value from given expression `op`, stores with key in document and returns that value.
   *  @param  key the key to test
   *  @param  op  the computation yielding the value to associate with `key`, if `key` is previously unbound.
   *  @return     the value associated with key (either previously or as a result of executing the method).
   */
  def getOrElseUpdate[B](key: String, op: => B)(implicit ev: B => BsonValue): BsonValue = {
    get(key) match {
      case Some(v) => v
      case None    => val d = op; this(key) = d; d
    }
  }

  // scalastyle:off method.name
  /**
   * Removes a key from this document.
   *  @param    key the key to be removed
   *  @return   the document itself.
   */
  def -=(key: String): Document = { underlying.remove(key); this }

  /**
   * Removes two or more elements from this document.
   *
   *  @param elem1 the first element to remove.
   *  @param elem2 the second element to remove.
   *  @param elems the remaining elements to remove.
   *  @return the document itself
   */
  def -=(elem1: String, elem2: String, elems: String*): Document = {
    this -= elem1
    this -= elem2
    this --= elems
  }

  /**
   * Removes all elements produced by an iterator from this $coll.
   *
   *  @param xs   the iterator producing the elements to remove.
   *  @return the $coll itself
   */
  def --=(xs: TraversableOnce[String]): Document = { xs foreach -=; this }
  // scalastyle:on method.name

  /**
   * Removes a key from this document, returning the value associated previously with that key as an option.
   *  @param    key the key to be removed
   *  @return   an option value containing the value associated previously with `key`,
   *            or `None` if `key` was not defined in the document before.
   */
  def remove(key: String): Option[BsonValue] = {
    val r = get(key)
    this -= key
    r
  }

  /**
   * Retains only those mappings for which the predicate `p` returns `true`.
   *
   * @param p  The test predicate
   */
  def retain(p: (String, BsonValue) => Boolean): Document = {
    for ((k, v) <- this)
      if (!p(k, v)) underlying.remove(k)
    this
  }

  /**
   * Removes all bindings from the document. After this operation has completed the document will be empty.
   */
  def clear() { underlying.clear(); }

  /**
   * Applies a transformation function to all values contained in this document.
   *  The transformation function produces new values from existing keys associated values.
   *
   * @param f  the transformation to apply
   * @return   the document itself.
   */
  def transform[B](f: (String, BsonValue) => B)(implicit ev: B => BsonValue): Document = {
    this.foreach(kv => update(kv._1, f(kv._1, kv._2)))
    this
  }

  /**
   * Copies the document and creates a new one
   *
   * @return a new document with a copy of the underlying BsonDocument
   */
  def copy(): Document = Document(copyBsonDocument())

}
