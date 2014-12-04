package org.mongodb.scala.core

import com.mongodb.async.SingleResultCallback

import scala.concurrent.{ Future, Promise }

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