package org.mongodb.scala.core

import com.mongodb.ReadPreference
import com.mongodb.async.SingleResultCallback
import com.mongodb.operation.{ AsyncBatchCursor, AsyncOperationExecutor, AsyncReadOperation }

import scala.collection.mutable
import scala.concurrent.Promise

trait MongoOperationIterableProvider[T] extends MongoIterable[T] with ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  val operation: AsyncReadOperation[AsyncBatchCursor[T]]

  val readPreference: ReadPreference

  val executor: AsyncOperationExecutor

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
