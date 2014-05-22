package org.mongodb.scala.core

import scala.language.higherKinds

/**
 * The RequiredTypesProvider trait
 *
 * Defines the types used across the system.
 *
 * - `Client`, `Database`, `Collection` and `CollectionView` are the implementation classes
 * - `ResultType`, `ListResultType` and `CursorType` are the future data types
 *
 */
trait RequiredTypesProvider {
  type Client <: MongoClientProvider
  type Database <: MongoDatabaseProvider
  type Collection[T] <: MongoCollectionProvider[T]
  type CollectionView[T] <: MongoCollectionViewProvider[T]
  type ResultType[T]
  type ListResultType[T]
  type CursorType[T]
}
