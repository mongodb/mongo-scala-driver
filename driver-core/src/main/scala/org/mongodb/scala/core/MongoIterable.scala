package org.mongodb.scala.core

import scala.collection.mutable

trait MongoIterable[T] {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * Helper to return the first item in the iterator or null.
   *
   * @return an option of T
   */
  def first(): ResultType[Option[T]]

  /**
   * Iterates over all documents in the view, applying the given block to each, and completing the returned future after all documents
   * have been iterated, or an exception has occurred.
   *
   * @param block    the block to apply to each document
   */
  def forEach(block: T => Unit): ResultType[Unit]

  /**
   * Iterates over all the documents, adding each to the given target.
   *
   * @param target   the collection to insert into
   */
  def into(target: mutable.Buffer[T]): ResultType[Unit]
}
