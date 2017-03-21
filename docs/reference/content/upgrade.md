+++
date = "2015-11-18T09:56:14Z"
title = "Changelog"
[menu.main]
  weight = 95
  pre = "<i class='fa fa-wrench'></i>"
+++

## Upgrade

### 2.0.0


#### MongoCollection mehtod default to collection type fix.
    
Previously, in the 1.x series `MongoCollection[T].find()` by default would return a `FindObservable[Document]` and not `FindObservable[T]`. 
While this was easy to work around by explicitly setting the type eg: `MongoCollection[T].find[T]()` we've bumped the version to 2.0.0 so 
that we can fix the API issue.

If you took advantage of the default type being `Document` you will need to update your code: `MongoCollection[T].find[Document]()`.

#### SingleObservable
    
The addition of the `SingleObservable` trait allows for easy identification of `Observables` that return only a single element. 
For a SingleObservables `toFuture()` will return a `Future[T]` instead of `Future[Seq[T]]`, any code relying on this will need to be 
updated to reflect the new result type.
