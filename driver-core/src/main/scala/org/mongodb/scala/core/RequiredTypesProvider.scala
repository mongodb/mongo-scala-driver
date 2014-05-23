package org.mongodb.scala.core

import scala.language.higherKinds
import org.mongodb.binding.ReferenceCounted
import org.mongodb.{MongoAsyncCursor, MongoFuture}

/**
 * The RequiredTypesAndTransformersProvider trait
 *
 * Defines the types used across the system.
 *
 * - `Client`, `Database`, `Collection` and `CollectionView` are the implementation classes
 * - `ResultType`, `ListResultType` and `CursorType` are the future data types
 *
 */
trait RequiredTypesAndTransformersProvider {

  /* Concrete Implementations */
  type Client <: MongoClientProvider
  type Database <: MongoDatabaseProvider
  type Collection[T] <: MongoCollectionProvider[T]
  type CollectionView[T] <: MongoCollectionViewProvider[T]

  /* Desired Data Types */
  type ResultType[T]
  type ListResultType[T]
  type CursorType[T]

  /* Transformers (Not robots in disguise but apply-to-all functions) */

  /**
   * A type converter method that converts a `MongoFuture[T]` to `ResultType[T]`
   *
   * Care should be taken to release the `binding` which is the [[ReferenceCounted]] type in the signature.
   *
   * @note `ResultType[T]` is defined by the concrete implementation of [[RequiredTypesAndTransformersProvider]]
   *
   * Converting to native Scala Futures:
   *
   * {{{
   *    protected def mongoFutureConverter[T]: (MongoFuture[T], ReferenceCounted) => Future[T] = {
   *     (result, binding) => {
   *       val promise = Promise[T]()
   *       result.register(new SingleResultCallback[T] {
   *         override def onResult(result: T, e: MongoException): Unit = {
   *           try {
   *             Option(e) match {
   *               case None => promise.success(result)
   *               case _ => promise.failure(e)
   *
   *             }
   *           }
   *           finally {
   *             binding.release()
   *           }
   *         }
   *       })
   *       promise.future
   *     }
   *   }
   * }}}
   *
   * @tparam T the type of result eg CommandResult, Document etc..
   * @return ResultType[T]
   */
  protected def mongoFutureConverter[T]: (MongoFuture[T], ReferenceCounted) => ResultType[T]

  /**
   * A type converter method that converts a `MongoFuture[MongoAsyncCursor[T]]` to `CursorType[T]`
   *
   * Care should be taken to release the `binding` which is the [[ReferenceCounted]] type in the signature.
   *
   * @note `CursorType[T]` is defined by the concrete implementation of [[RequiredTypesAndTransformersProvider]]
   *
   * Converting to native Scala Futures:
   *
   * {{{
   *  protected def mongoCursorConverter[T]: (MongoFuture[MongoAsyncCursor[T]], ReferenceCounted) => Future[MongoAsyncCursor[T]] = {
   *    (result, binding) =>
   *      val promise = Promise[MongoAsyncCursor[T]]()
   *
   *      result.register(new SingleResultCallback[MongoAsyncCursor[T]] {
   *        override def onResult(result: MongoAsyncCursor[T], e: MongoException): Unit = {
   *          try {
   *            Option(e) match {
   *              case None => promise.success(result)
   *              case _ => promise.failure(e)
   *
   *            }
   *          }
   *          finally {
   *            binding.release()
   *          }
   *        }
   *      })
   *      promise.future
   *  }
   * }}}
   *
   * @tparam T the type of result eg CommandResult, Document etc..
   * @return CursorType[T]
   */
  protected def mongoCursorConverter[T]: (MongoFuture[MongoAsyncCursor[T]], ReferenceCounted) => CursorType[T]

  /**
   * A type transformer that converts a `ResultType[List[T\]\]` to `ListResultType[T]`
   *
   * Depending on the required `ListResultType` this may require no transformation:
   * {{{
   *   { result => result }
   * }}}
   *
   * @tparam T List data type of item eg Document or String
   * @return the correct ListResultType[T]
   */
  protected def listToListResultTypeConverter[T]: ResultType[List[T]] => ListResultType[T]

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
  protected def voidToUnitConverter: ResultType[Void] => ResultType[Unit]

}
