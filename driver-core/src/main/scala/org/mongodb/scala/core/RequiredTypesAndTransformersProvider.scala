package org.mongodb.scala.core

import java.util

import com.mongodb.async.SingleResultCallback
import com.mongodb.client.model.FindOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.{ AsyncBatchCursor, AsyncOperationExecutor, AsyncReadOperation }
import com.mongodb.{ MongoNamespace, ReadPreference }

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.{ Future, Promise }
import scala.language.higherKinds
import scala.util.{ Failure, Success, Try }

/**
 * The RequiredTypesAndTransformersProvider trait
 *
 * Defines the types used across the system.
 *
 * - `Client`, `Database`, `Collection`, `FindFluent` and `OperationIterable` are the implementation classes
 * - `ResultType`, `ListResultType` are the future data types
 *
 */
trait RequiredTypesAndTransformersProvider {

  /* Concrete Implementations */
  type Client <: MongoClientProvider
  type Database <: MongoDatabaseProvider
  type Collection[T] <: MongoCollectionProvider[T]
  type FindFluent[T] <: MongoCollectionFindFluentProvider[T]
  type OperationIterable[T] <: MongoOperationIterableProvider[T]

  /* Desired Data Types */
  type ResultType[T]
  type ListResultType[T]

  /* Required  */
  protected def findFluent[T](namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                              options: OperationOptions, executor: AsyncOperationExecutor,
                              clazz: Class[T]): FindFluent[T]

  protected def operationIterable[T](operation: AsyncReadOperation[AsyncBatchCursor[T]],
                                     readPreference: ReadPreference, executor: AsyncOperationExecutor,
                                     clazz: Class[T]): OperationIterable[T]

  /* Transformers (Not robots in disguise but apply-to-all functions) */

  /**
   * A type transformer that converts a `ResultType[List[T]]` to `ListResultType[T]`
   *
   * Depending on the required `ListResultType` this may require no transformation:
   * {{{
   *   { result => result }
   * }}}
   *
   * @tparam T List data type of item eg Document or String
   * @return the correct ListResultType[T]
   */
  protected def listResultTypeConverter[T](): Future[List[T]] => ListResultType[T]

  /**
   * A type transformer that takes a `ResultType[Void]` and converts it to `ResultType[Unit]`
   *
   * This is needed as returning `Void` is not idiomatic in scala whereas a `Unit` is more acceptable.
   *
   * For scala Futures an example is:
   * {{{
   *   result => result.mapTo[Unit]
   * }}}
   *
   * @note Each MongoDatabaseAdmin implementation must provide this.
   *
   * @return ResultType[Unit]
   */
  protected def resultTypeConverter[T](): Future[T] => ResultType[T]

  /* Callbacks - to hold the future and apply the transformer */

  protected def resultCallback[T, R](mapper: T => R) = new ResultCallback[T, R, ResultType[R]] {
    val map: (T) => R = mapper
    val transformer = resultTypeConverter[R]()
  }

  protected def listToListResultTypeCallback[T]() = new ResultCallback[util.List[T], List[T], ListResultType[T]] {
    val map: (util.List[T]) => List[T] = { result => result.asScala.toList }
    val transformer = listResultTypeConverter[T]()
  }

  protected def resultTypeCallback[T]() = new ResultCallback[T, T, ResultType[T]] {
    val map: T => T = result => result
    val transformer = resultTypeConverter[T]()
  }

  protected def voidToUnitResultTypeCallback() = new ResultCallback[Void, Unit, ResultType[Unit]] {
    val map: Void => Unit = result => Unit
    val transformer = resultTypeConverter[Unit]()

  }

  protected def ignoreResultTypeCallback[T]() = new ResultCallback[T, Unit, ResultType[Unit]] {
    val map: T => Unit = result => Unit
    val transformer = resultTypeConverter[Unit]()
  }

  protected def asyncBatchCursorToUnitCallback[T](block: T => Unit) =
    new ResultCallback[AsyncBatchCursor[T], Unit, ResultType[Unit]] {
      val map: (AsyncBatchCursor[T]) => Unit = (batchCursor: AsyncBatchCursor[T]) =>
        loopCursor(batchCursor, block, ignoreResultTypeCallback())
      val transformer = resultTypeConverter[Unit]()
    }

  protected def asyncBatchCursorIntoCallback[T](target: mutable.Buffer[T], promise: Promise[Unit]) =
    new SingleResultCallback[AsyncBatchCursor[T]] {
      def onResult(batchCursor: AsyncBatchCursor[T], t: Throwable): Unit =
        Option(t) match {
          case None => loopCursor(batchCursor, (result: T) => target += result, new SingleResultCallback[Void]() {
            override def onResult(result: Void, t: Throwable): Unit =
              Option(t) match {
                case None => promise.success(None)
                case _    => promise.failure(t)
              }
          })
          case _ => promise.failure(t)
        }
    }

  protected def asyncBatchCursorIntoCallback[T](target: mutable.Buffer[T]) = {
    new ResultCallback[AsyncBatchCursor[T], Unit, ResultType[Unit]] {
      val callback: SingleResultCallback[Void] = new SingleResultCallback[Void]() {
        override def onResult(result: Void, t: Throwable): Unit = {
          Option(t) match {
            case None => promise.success(None)
            case _    => promise.failure(t)
          }
        }
      }
      val map: (AsyncBatchCursor[T]) => Unit = (batchCursor: AsyncBatchCursor[T]) =>
        loopCursor(batchCursor, (result: T) => target += result, callback)
      val transformer: (Future[Unit]) => ResultType[Unit] = resultTypeConverter[Unit]()
    }
  }

  protected def asyncBatchCursorFirstOptionCallback[T]() =
    new ResultCallback[AsyncBatchCursor[T], Option[T], ResultType[Option[T]]] {
      val map: (AsyncBatchCursor[T]) => Option[T] = (cursor) => None
      val transformer: (Future[Option[T]]) => ResultType[Option[T]] = resultTypeConverter[Option[T]]()
      override def onResult(batchCursor: AsyncBatchCursor[T], t: Throwable): Unit = {
        Option(t) match {
          case None =>
            batchCursor.setBatchSize(1)
            batchCursor.next(new SingleResultCallback[util.List[T]] {
              override def onResult(result: util.List[T], t: Throwable): Unit = {
                Option(t) match {
                  case None => promise.success(Option(result.get(0)))
                  case _    => promise.failure(t)
                }
              }
            })
          case _ => promise.failure(t)
        }
      }
    }

  private def loopCursor[T](batchCursor: AsyncBatchCursor[T], block: T => Unit, callback: SingleResultCallback[Void]) {
    batchCursor.next(new SingleResultCallback[util.List[T]] {
      override def onResult(results: util.List[T], t: Throwable): Unit = {
        (Option(t), Option(results)) match {
          case (None, Some(_)) =>
            Try({
              for (result <- results.asScala) {
                block(result)
              }
              loopCursor(batchCursor, block, callback)
            }) match {
              case Failure(f) =>
                batchCursor.close()
                callback.onResult(null, f)
              case Success(_) =>
            }
          case _ =>
            batchCursor.close()
            callback.onResult(null, t)
        }
      }

    })
  }

}
