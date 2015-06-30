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

package org.mongodb.scala

import scala.collection.mutable.ListBuffer
import scala.util.Try

import org.mongodb.scala.internal._

/**
 * Extends the Java [[Observable]] and adds helpers to make Observables composable and simple to Subscribe to.
 *
 * Automatically imported into the `org.mongodb.scala` namespace
 */
trait ObservableImplicits {

  /**
   * Extends the Java [[Observable]] and adds helpers to make Observables composable and simple to Subscribe to.
   *
   * @param observable the original Observable
   * @tparam T the type of result the Observable emits
   *
   * @define forComprehensionExamples
   *         Example:
   *
   *         {{{
   *             def f = Observable(1 to 10)
   *             def g = Observable(100 to 100)
   *             val h = for {
   *               x: Int <- f // returns Observable(1 to 10)
   *               y: Int <- g // returns Observable(100 to 100)
   *             } yield x + y
   *         }}}
   *
   *         is translated to:
   *
   *         {{{
   *            f flatMap { (x: Int) => g map { (y: Int) => x + y } }
   *         }}}
   */
  implicit class ScalaObservable[T](observable: Observable[T]) {

    /**
     * Subscribes to the [[Observable]] and requests `Long.MaxValue`.
     *
     * @param doOnNext anonymous function to apply to each emitted element.
     */
    def subscribe(doOnNext: T => Any): Unit = subscribe(doOnNext, t => t)

    /**
     * Subscribes to the [[Observable]] and requests `Long.MaxValue`.
     *
     * @param doOnNext anonymous function to apply to each emitted element.
     * @param doOnError anonymous function to apply if there is an error.
     */
    def subscribe(doOnNext: T => Any, doOnError: Throwable => Any): Unit = subscribe(doOnNext, doOnError, () => ())

    /**
     * Subscribes to the [[Observable]] and requests `Long.MaxValue`.
     *
     * Uses the default or overridden `onNext`, `onError`, `onComplete` partial functions.
     *
     * @param doOnNext anonymous function to apply to each emitted element.
     * @param doOnError anonymous function to apply if there is an error.
     * @param doOnComplete anonymous function to apply on completion.
     */
    def subscribe(doOnNext: T => Any, doOnError: Throwable => Any, doOnComplete: () => Any): Unit = {
      observable.subscribe(new Observer[T] {
        override def onSubscribe(subscription: Subscription): Unit = subscription.request(Long.MaxValue)

        override def onNext(tResult: T): Unit = doOnNext(tResult)

        override def onError(throwable: Throwable): Unit = doOnError(throwable)

        override def onComplete(): Unit = doOnComplete()

      })
    }

    /* Monadic operations */

    /**
     * Applies a function applied to each emitted result.
     *
     * Automatically requests all results
     *
     * @param doOnEach the anonymous function applied to each emitted item
     * @tparam U the resulting type after the transformation
     */
    def foreach[U](doOnEach: T => U): Unit = subscribe(doOnEach)

    /**
     * Creates a new Observable by applying the `resultFunction` function to each emitted result.
     * If there is an error and `onError` is called the `errorFunction` function is applied to the failed result.
     *
     * @param  mapFunction function that transforms a each result of the receiver and passes the result to the returned Observable
     * @param  errorMapFunction  function that transforms a failure of the receiver into a failure of the returned observer
     * @tparam S the resulting type of each item in the Observable
     * @return    an Observable with transformed results and / or error.
     */
    def transform[S](mapFunction: T => S, errorMapFunction: Throwable => Throwable): Observable[S] =
      MapObservable(observable, mapFunction, errorMapFunction)

    /**
     * Creates a new Observable by applying a function to each emitted result of the [[Observable]].
     * If the Observable calls errors then then the new Observable will also contain this exception.
     *
     * $forComprehensionExamples
     *
     * @param  mapFunction function that transforms a each result of the receiver and passes the result to the returned Observable
     * @tparam S the resulting type of each item in the Observable
     * @return    an Observable with transformed results and / or error.
     */
    def map[S](mapFunction: T => S): Observable[S] = MapObservable(observable, mapFunction)

    /**
     * Creates a new Observable by applying a function to each emitted result of the [[Observable]].
     * If the Observable calls errors then then the new Observable will also contain this exception.
     *
     * As each emitted item passed to `onNext` returns an Observable, we tightly control the requests to the parent Observable.
     * The requested amount is then passed to the child Observable and only when that is completed does the  parent become available for
     * requesting more data.
     *
     * $forComprehensionExamples
     *
     * @param  mapFunction function that transforms a each result of the receiver into an Observable and passes each result of that
     *                     Observable to the returned Observable.
     * @tparam S the resulting type of each item in the Observable
     * @return    an Observable with transformed results and / or error.
     */
    def flatMap[S](mapFunction: T => Observable[S]): Observable[S] = FlatMapObservable(observable, mapFunction)

    /**
     * Creates a new [[Observable]] by filtering the value of the current Observable with a predicate.
     *
     * If the current Observable fails, then the resulting Observable also fails.
     *
     * Example:
     * {{{
     *  val oddValues = Observable(1 to 100) filter { _ % 2 == 1 }
     * }}}
     *
     * @param predicate the function that is applied to each result emitted if it matches that result is passes to the returned Observable
     * @return an Observable only containing items matching that match the predicate
     */
    def filter(predicate: T => Boolean): Observable[T] = FilterObservable(observable, predicate)

    /**
     * Used by for-comprehensions.
     */
    final def withFilter(p: T => Boolean): Observable[T] = FilterObservable(observable, p)

    /**
     * Collects all the values of the [[Observable]] into a list and returns a new Observable with that list.
     *
     * Example:
     * {{{
     *  val listOfNumbers = Observable(1 to 100).collect()
     * }}}
     *
     * @note If the Observable is large then this will consume lots of memory!
     *       If the underlying Observable is infinite this Observable will never complete.
     * @see Uses [[foldLeft]] underneath
     * @return an Observable that emits a single item, the result of accumulator.
     */
    def collect[S](): Observable[List[T]] =
      FoldLeftObservable(observable, ListBuffer[T](), (l: ListBuffer[T], v: T) => l += v).map(_.toList)

    /**
     * Creates a new [[Observable]] that contains the single result of the applied accumulator function.
     *
     * The first item emitted by the Observable is passed to the supplied accumulator function alongside the initial value, then all other
     * emitted items are passed along with the previous result of the accumulator function.
     *
     * Example:
     * {{{
     *  val countingObservable = Observable(1 to 100) foldLeft(0)((v, i) => v + 1)
     * }}}
     *
     * @note If this function is used to collect results into a collection then it could use lots of memory!
     *       If the underlying Observable is infinite this Observable will never complete.
     * @param initialValue the initial (seed) accumulator value
     * @param accumulator an accumulator function to be invoked on each item emitted by the source Observable, the result of which will be
     *                    used in the next accumulator call.
     * @return an Observable that emits a single item, the result of accumulator.
     */
    def foldLeft[S](initialValue: S)(accumulator: (S, T) => S): Observable[S] = FoldLeftObservable(observable, initialValue, accumulator)

    /**
     * Creates a new [[Observable]] that will handle any matching throwable that this Observable might contain.
     * If there is no match, or if this Observable contains a valid result then the new Observable will contain the same.
     *
     * Example:
     *
     * {{{
     *  mongoExceptionObservable recover { case e: MongoException => 0 } // final result: 0
     *  mongoExceptionObservable recover { case e: NotFoundException => 0 } // result: exception
     * }}}
     *
     * @param pf the partial function used to pattern match against the `onError` throwable
     * @tparam U the type of the returned Observable
     * @return an Observable that will handle any matching throwable and not error.
     */
    def recover[U >: T](pf: PartialFunction[Throwable, U]): Observable[U] = RecoverObservable(observable, pf)

    /**
     * Creates a new [[Observable]] that will handle any matching throwable that this Observable might contain by assigning it a value
     * of another Observable.
     *
     * If there is no match, or if this Observable contains a valid result then the new Observable will contain the same result.
     *
     * Example:
     *
     * {{{
     *  successfulObservable recoverWith { case e: ArithmeticException => observableB } // result: successfulObservable
     *  mongoExceptionObservable recoverWith { case t: Throwable => observableB } // result: observableB
     * }}}
     *
     * == Ensuring results from a Single Observer ==
     *
     * `recoverWith` can potentially emit results from either Observer. This often isn't desirable, so to ensure only a single Observable
     * issues results combine with the [[collect()]] method eg:
     *
     * {{{
     *  val results = Observable(1 to 100).collect() { case t: Throwable => observable(200 to 300).collect() }
     * }}}
     *
     * @param pf the partial function used to pattern match against the `onError` throwable
     * @tparam U the type of the returned Observable
     * @return an Observable that will handle any matching throwable and not error but recover with a new observable
     */
    def recoverWith[U >: T](pf: PartialFunction[Throwable, Observable[U]]): Observable[U] = RecoverWithObservable(observable, pf)

    /**
     * Zips the values of `this` and `that` [[Observable]], and creates a new Observable holding the tuple of their results.
     *
     * If `this` Observable fails, the resulting Observable is failed with the throwable stored in `this`. Otherwise, if `that`
     * Observable fails, the resulting Observable is failed with the throwable stored in `that`.
     *
     * It will only emit as many items as the number of items emitted by the source Observable that emits the fewest items.
     *
     * @param that the Observable to zip with
     * @tparam U the type of the `that` Observable
     * @return a new zipped Observable
     */
    def zip[U](that: Observable[U]): Observable[(T, U)] = ZipObservable(observable, that)

    /**
     * Creates a new [[Observable]] which returns the results of this Observable, if there is an error, it will then fallback to returning
     * the results of the alternative "`that`" Observable.
     *
     * If both Observables fail, the resulting Observable holds the throwable object of the first Observable.
     *
     * Example:
     * {{{
     *  val fallBackObservable = Observable(1 to 100) fallbackTo Observable(200 to 300)
     * }}}
     *
     * == Ensuring results from a Single Observer ==
     *
     * `fallbackTo` can potentially emit results from either Observer. This often isn't desirable, so to ensure only a single Observable
     * issues results combine with the [[collect()]] method eg:
     *
     * {{{
     *  val results = Observable(1 to 100).collect() fallbackTo observable(200 to 300).collect()
     * }}}
     *
     * @param that the Observable to fallback to if `this` Observable fails
     * @tparam U the type of the returned Observable
     * @return an Observable that will fallback to the `that` Observable should `this` Observable complete with an `onError`.
     */
    def fallbackTo[U >: T](that: Observable[U]): Observable[U] = RecoverWithObservable(observable, { case t: Throwable => that }, true)

    /**
     * Applies the side-effecting function to the final result of this [[Observable]] and, returns a new Observable with the result of
     * this Observable.
     *
     * This method allows one to enforce that the callbacks are executed in a specified order.
     *
     * Note that if one of the chained `andThen` callbacks throws an exception, that exception is not propagated to the subsequent
     * `andThen` callbacks. Instead, the subsequent `andThen` callbacks are given the original value of this Observable.
     *
     * The following example prints out `10`:
     *
     * {{{
     *  observable(1 to 10) andThen {
     *   case r => sys.error("runtime exception")
     *  } andThen {
     *   case Success(x) => println(x)
     *   case Failure(t) => println("Failure")
     *  }
     * }}}
     *
     * @param pf the partial function to pattern match against
     * @tparam U the result type of the
     * @return an
     */
    def andThen[U](pf: PartialFunction[Try[T], U]): Observable[T] = AndThenObservable(observable, pf)
  }
}
