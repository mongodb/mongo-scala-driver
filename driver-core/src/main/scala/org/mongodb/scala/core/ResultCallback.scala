package org.mongodb.scala.core

import com.mongodb.async.SingleResultCallback

import scala.concurrent.{ Future, Promise }

/**
 * A transformative implementation of a SingleResultCallback
 *
 * @tparam T the initial callback type
 * @tparam R the result of the mapped type from the callback type
 * @tparam F the final transformed result
 */
trait ResultCallback[T, R, F] extends SingleResultCallback[T] {
  val map: T => R
  val transformer: Future[R] => F
  val promise = Promise[R]()

  def result: F = transformer(promise.future)

  def onResult(result: T, t: Throwable): Unit = {
    Option(t) match {
      case None => promise.success(map(result))
      case _    => promise.failure(t)
    }
  }
}