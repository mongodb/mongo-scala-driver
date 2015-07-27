+++
date = "2015-07-19T14:27:51-04:00"
title = "Integrations"
[menu.main]
  identifier = "Integrations"
  weight = 60
  pre = "<i class='fa fa-arrows-h'></i>"
+++

# Integrations

The `Observable`, `Observer` and `Subscription` implementation draws inspiration from the [ReactiveX](http://reactivex.io/) and [reactive streams](http://www.reactive-streams.org) libraries and provides easy interoperability with them.  For more information about these classes please see the [quick tour primer]({{< relref "getting-started/quick-tour-primer.md" >}}).

## RxScala

{{< img src="/img/mongoRxLogo.png" title="RxScala" class="align-left" >}}

The [ReactiveX](http://reactivex.io/) scala driver (RxScala) provides extra composability of `Observables` compared to the MongoDB `Observable` implementation.

An example implicit based implementation can be found in the [examples folder]({{< srcref "examples/src/test/scala/rxScala">}}).  This includes an implicit based conversion from `Observable` to `rx.Observable` and an example of it being used.  

The example uses the [`Observable.apply(f: (Subscriber[T]) => Unit)`](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Observable$@apply[T]\(f:rx.lang.scala.Subscriber[T]=>Unit\):rx.lang.scala.Observable[T]) method to return a cold observable.  It also implements the [`Producer`](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Producer) trait, so the `Observable` can honor "Backpressure" when when using any of the [backpressure operators](http://reactivex.io/documentation/operators/backpressure.html).


## Reactive Streams

{{< img src="/img/mongoReactiveLogo.png" title="RxScala" class="align-right" >}}

The [Reactive streams](http://www.reactive-streams.org) initiative provides interfaces that allow reactive stream based systems to interact. The API is similar to the MongoDB `Observable` API but without the composability of the MongoDB implementation.  
 
Converting from an `Observable` to a `Publisher` is a simple process and can be done in a few short lines of code. An implicit based conversion can be found in the [examples folder]({{< srcref "examples/src/test/scala/rxStreams">}}).
