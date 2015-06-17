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

package org.mongodb.scala.collection

import scala.collection.JavaConverters._
import scala.collection.{GenTraversableOnce, Traversable}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

import org.bson._
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.mongodb.scala.Helpers.DefaultsTo

/**
 * Base Document trait.
 *
 * A strictly typed `Traversable[(String, BsonValue)]` and provides the underlying immutable document behaviour.
 * See [[immutable.Document]] or [[mutable.Document]] for the concrete implementations.
 *
 * @tparam T The concrete Document implementation
 */
trait BaseDocument[T] extends Traversable[(String, BsonValue)] with Bson {

  /**
   * The underlying bson document
   *
   * Restricted access to the underlying BsonDocument
   */
  protected[scala] val underlying: BsonDocument

  /**
   * Create a concrete document instance
   *
   * @param underlying the underlying BsonDocument
   * @return a concrete document instance
   */
  protected[scala] def apply(underlying: BsonDocument): T

  /**
   * Retrieves the value which is associated with the given key or throws a `NoSuchElementException`.
   *
   * @param  key the key
   * @return     the value associated with the given key, or throws `NoSuchElementException`.
   */
  def apply[TResult <: BsonValue](key: String)(implicit e: TResult DefaultsTo BsonValue, ct: ClassTag[TResult]): TResult = {
    get[TResult](key) match {
      case Some(value) => value
      case None        => throw new NoSuchElementException("key not found: " + key)
    }
  }

  /**
   * Returns the value associated with a key, or a default value if the key is not contained in the map.
   * @param   key      the key.
   * @param   default  a computation that yields a default value in case no binding for `key` is
   *                   found in the map.
   * @tparam  B        the result type of the default computation.
   * @return  the value associated with `key` if it exists,
   *          otherwise the result of the `default` computation.
   */
  def getOrElse[B >: BsonValue](key: String, default: => B): B = get(key) match {
    case Some(v) => v
    case None    => default
  }

  //scalastyle:off spaces.after.plus  method.name
  /**
   * Creates a new document containing a new key/value and all the existing key/values.
   *
   * Mapping `kv` will override existing mappings from this document with the same key.
   *
   * @param kv    the key/value mapping to be added
   * @return      a new document containing mappings of this document and the mapping `kv`.
   */
  def +[B](kv: (String, B))(implicit ev: B => BsonValue): T = {
    val bsonDocument: BsonDocument = copyBsonDocument()
    bsonDocument.put(kv._1, kv._2)
    apply(bsonDocument)
  }

  /**
   * Adds two or more elements to this document and returns a new document.
   *
   * @param elem1 the first element to add.
   * @param elem2 the second element to add.
   * @param elems the remaining elements to add.
   * @param ev implicit evidence that type `B` is convertable to a `BsonValue`
   * @tparam B the type being added
   * @return A new document with the new key values added.
   */
  def +[B](elem1: (String, B), elem2: (String, B), elems: (String, B)*)(implicit ev: B => BsonValue): T = {
    val bsonDocument: BsonDocument = copyBsonDocument()
    bsonDocument.put(elem1._1, elem1._2)
    bsonDocument.put(elem2._1, elem2._2)
    elems.foreach(kv => bsonDocument.put(kv._1, kv._2))
    apply(bsonDocument)
  }

  /**
   * Adds a number of elements provided by a traversable object and returns a new document with the added elements.
   *
   * @param xs      the traversable object consisting of key-value pairs.
   * @return        a new document with the bindings of this document and those from `xs`.
   */
  def ++[B](xs: GenTraversableOnce[(String, B)])(implicit ev: B => BsonValue): T = {
    val bsonDocument = copyBsonDocument()
    xs.foreach(kv => bsonDocument.put(kv._1, kv._2))
    apply(bsonDocument)
  }
  //scalastyle:on spaces.after.plus

  /**
   * Removes a key from this document, returning a new document.
   *
   * @param    key the key to be removed
   * @return   a new document without a binding for `key`
   */
  def -(key: String): T = {
    val newUnderlying = new BsonDocument()
    for ((k, v) <- iterator if k != key) {
      newUnderlying.put(k, v)
    }
    apply(newUnderlying)
  }

  /**
   * Removes two or more elements to this document and returns a new document.
   *
   * @param elem1 the first element to remove.
   * @param elem2 the second element to remove.
   * @param elems the remaining elements to remove.
   * @return A new document with the keys removed.
   */
  def -(elem1: String, elem2: String, elems: String*): T = --(List(elem1, elem2) ++ elems)

  /**
   * Removes a number of elements provided by a traversable object and returns a new document without the removed elements.
   *
   * @param xs      the traversable object consisting of key-value pairs.
   * @return        a new document with the bindings of this document and those from `xs`.
   */
  def --(xs: GenTraversableOnce[String]): T = {
    val keysToIgnore = xs.toList
    val newUnderlying = new BsonDocument()
    for ((k, v) <- iterator if !keysToIgnore.contains(k)) {
      newUnderlying.put(k, v)
    }
    apply(newUnderlying)
  }
  // scalastyle:on method.name

  /**
   * Creates a new Document consisting of all key/value pairs of the current document
   * plus a new pair of a given key and value.
   *
   * @param key    The key to add
   * @param value  The new value
   * @return       A fresh immutable document with the binding from `key` to `value` added to the new document.
   */
  def updated[B](key: String, value: B)(implicit ev: B => BsonValue): T = this + ((key, value))

  /**
   * Creates a new Document consisting of all key/value pairs of the current document
   * plus a new pair of a given key and value.
   *
   * @param kv    The key/value to add
   * @return       A fresh immutable document with the binding from `key` to `value` added to the new document.
   */
  def updated[B](kv: (String, B))(implicit ev: B => BsonValue): T = this + kv

  /**
   * Optionally returns the value associated with a key.
   *
   * @param  key  the key we want to lookup
   * @return an option value containing the value associated with `key` in this document,
   *         or `None` if none exists.
   */
  def get[TResult <: BsonValue](key: String)(implicit e: TResult DefaultsTo BsonValue, ct: ClassTag[TResult]): Option[TResult] = {
    underlying.containsKey(key) match {
      case true => Try(ct.runtimeClass.cast(underlying.get(key))) match {
        case Success(v)  => Some(v.asInstanceOf[TResult])
        case Failure(ex) => None
      }
      case false => None
    }
  }

  /**
   * Creates a new iterator over all key/value pairs in this document
   *
   * @return the new iterator
   */
  def iterator: Iterator[(String, BsonValue)] = underlying.asScala.iterator

  /**
   * Filters this document by retaining only keys satisfying a predicate.
   * @param  p   the predicate used to test keys
   * @return a new document consisting only of those key value pairs of this map where the key satisfies
   *         the predicate `p`.
   */
  def filterKeys(p: String => Boolean): T = this -- keys.filterNot(p)

  /**
   * Tests whether this map contains a binding for a key
   *
   * @param key the key
   * @return true if there is a binding for key in this document, false otherwise.
   */
  def contains(key: String): Boolean = underlying.containsKey(key)

  /**
   * Collects all keys of this document in a set.
   *
   * @return  a set containing all keys of this document.
   */
  def keySet: Set[String] = underlying.keySet().asScala.toSet

  /**
   * Collects all keys of this document in an iterable collection.
   *
   * @return the keys of this document as an iterable.
   */
  def keys: Iterable[String] = keySet.toIterable

  /**
   * Creates an iterator for all keys.
   *
   * @return an iterator over all keys.
   */
  def keysIterator: Iterator[String] = keySet.toIterator

  /**
   * Collects all values of this document in an iterable collection.
   *
   * @return the values of this document as an iterable.
   */
  def values: Iterable[BsonValue] = underlying.values().asScala

  /**
   * Creates an iterator for all values in this document.
   *
   * @return an iterator over all values that are associated with some key in this document.
   */
  def valuesIterator: Iterator[BsonValue] = values.toIterator

  /**
   * Gets a JSON representation of this document
   *
   * @return a JSON representation of this document
   */
  def toJson: String = underlying.toJson

  /**
   * Returns a copy of the underlying BsonDocument
   */
  def toBsonDocument: BsonDocument = copyBsonDocument()

  override def toBsonDocument[TDocument](documentClass: Class[TDocument], codecRegistry: CodecRegistry): BsonDocument = underlying

  /**
   * Copies the BsonDocument
   * @return the copied BsonDocument
   */
  private[collection] def copyBsonDocument(): BsonDocument = {
    val bsonDocument = new BsonDocument()
    for (entry <- underlying.entrySet().asScala) bsonDocument.put(entry.getKey, entry.getValue)
    bsonDocument
  }

}
