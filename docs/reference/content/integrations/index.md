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

{{< img src="/img/mongoRxLogo.png" title="RxScala" class="align-right" >}}

The [ReactiveX](http://reactivex.io/) scala driver (RxScala) provides extra composability compared to the MongoDB implementation of `Observerable`.  To implement an `rx.Observable` use the [`Observable.apply(f: (Subscriber[T]) => Unit)`](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Observable$@apply[T]\(f:rx.lang.scala.Subscriber[T]=>Unit\):rx.lang.scala.Observable[T]) method. This returns an Observable that will execute the specified function when someone subscribes to it, this follows the native MongoDB implementation. By implementing the [`Producer`](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Producer) trait you can also honor "Backpressure" and use the RxScala [backpressure operators](http://reactivex.io/documentation/operators/backpressure.html).

An example implicit based implementation can be found in the [examples folder]({{< srcref "examples/src/test/scala/rxScala">}}).  This includes an implicit based conversion from `Observable` to `rx.Observable` and an example of it being used.


## Reactive Streams

{{< img src="/img/mongoReactiveLogo.png" title="RxScala" class="align-right" >}}

 The [Reactive streams](http://www.reactive-streams.org) initiative provides API's so that reactive stream based systems can interact. The API is similar to the MongoDB `Observable` API but without the composability of the MongoDB implementation.  
 
 Converting a `Observable` to a `Publisher` is a simple process and can be done in a few short lines of code.

An example implicit based implementation can be found in the [examples folder]({{< srcref "examples/src/test/scala/rxStreams">}}).  This includes an implicit based conversion from `Observable` to `Producer` and an example of it being used.
