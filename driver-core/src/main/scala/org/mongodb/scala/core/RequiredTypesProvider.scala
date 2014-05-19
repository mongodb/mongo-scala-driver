package org.mongodb.scala.core

import scala.language.higherKinds

trait RequiredTypesProvider {
  type Client <: MongoClientProvider
  type Database <: MongoDatabaseProvider
  type Collection[T] <: MongoCollectionProvider[T]
  type CollectionView[T] <: MongoCollectionViewProvider[T]
  type ResultType[T]
  type ListResultType[T]
  type CursorType[T]
}
