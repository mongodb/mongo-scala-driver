package org.mongodb.scala.core

import com.mongodb.ReadPreference
import com.mongodb.operation.{AsyncBatchCursor, AsyncOperationExecutor, AsyncReadOperation}

import scala.collection.mutable
import scala.concurrent.Promise

trait MongoOperationIterableProvider[T] extends MongoIterable[T] with ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  /**
   * The operation to be iterated
   *
   * @note Its expected that the MongoOperationIterable implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val operation: AsyncReadOperation[AsyncBatchCursor[T]]

  /**
   * The Read Preference to use when executing the underlying operation
   *
   * @note Its expected that the MongoOperationIterable implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val readPreference: ReadPreference

  /**
   * The AsyncOperationExecutor to be used with this MongoCollection instance
   *
   * @note Its expected that the MongoOperationIterable implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val executor: AsyncOperationExecutor

  /**
   * The class to decode each document into
   *
   * @note Its expected that the MongoOperationIterable implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val clazz: Class[T]

  /**
   * Helper to return the first item in the iterator or null.
   *
   * @return an option of T
   */
  override def first(): ResultType[Option[T]] =
    executeAsync(operation, readPreference, asyncBatchCursorFirstOptionCallback[T]())

  /**
   * Iterates over all documents in the view, applying the given block to each, and completing the returned future after all documents
   * have been iterated, or an exception has occurred.
   *
   * @param block    the block to apply to each document
   */
  override def forEach(block: (T) => Unit): ResultType[Unit] =
    executeAsync(operation, readPreference, asyncBatchCursorToUnitCallback[T](block))

  /**
   * Iterates over all the documents, adding each to the given target.
   *
   * @param target   the collection to insert into
   */
  override def into(target: mutable.Buffer[T]): ResultType[Unit] = {
    val promise = Promise[Unit]()
    executor.execute(operation, readPreference, asyncBatchCursorIntoCallback(target, promise))
    resultTypeConverter()(promise.future)
  }

}
