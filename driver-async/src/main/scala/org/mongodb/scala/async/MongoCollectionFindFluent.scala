package org.mongodb.scala.async

import com.mongodb.MongoNamespace
import com.mongodb.client.model.FindOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.connection.Cluster
import com.mongodb.operation.AsyncOperationExecutor
import org.mongodb.scala.core.{MongoCollectionFindFluentProvider, MongoClientOptions}


/**
 *
 * @param namespace
 * @param filter
 * @param findOptions
 * @param options
 * @param executor
 * @param clazz
 * @tparam T
 */
case class MongoCollectionFindFluent[T](namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                                        options: OperationOptions, executor: AsyncOperationExecutor, clazz: Class[T])
  extends MongoCollectionFindFluentProvider[T] with RequiredTypesAndTransformers {

  /**
   * A copy method to produce a new updated version of a `FindFluent`
   *
   * @param filter the filter, which may be null.
   * @param findOptions the options to apply to a find operation (also commonly referred to as a query).
   * @param options the OperationOptions for the find
   * @param executor the execution handler for the find operation
   * @param clazz the resulting class type
   * @return
   */
  override protected def copy(namespace: MongoNamespace, filter: Any, findOptions: FindOptions,
                              options: OperationOptions, executor: AsyncOperationExecutor,
                              clazz: Class[T]): FindFluent[T] =
    MongoCollectionFindFluent(namespace, filter, findOptions, options, executor, clazz)
}
