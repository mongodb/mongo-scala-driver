package org.mongodb.scala.core

import java.util.concurrent.TimeUnit._

import com.mongodb.MongoNamespace
import com.mongodb.client.model.FindOptions
import com.mongodb.client.options.OperationOptions
import com.mongodb.operation.{ AsyncOperationExecutor, FindOperation }
import org.bson.codecs.Codec
import org.bson.{ BsonDocument, BsonDocumentWrapper }

import scala.collection.mutable
import scala.concurrent.duration.Duration

trait MongoCollectionFindFluentProvider[T] extends MongoIterable[T] with ExecutorHelper {

  this: RequiredTypesAndTransformersProvider =>

  val namespace: MongoNamespace

  val filter: Any

  val findOptions: FindOptions

  val options: OperationOptions

  val executor: AsyncOperationExecutor

  val clazz: Class[T]

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
  protected def copy(namespace: MongoNamespace, filter: Any, findOptions: FindOptions, options: OperationOptions,
                     executor: AsyncOperationExecutor, clazz: Class[T]): FindFluent[T]

  def filter(filter: Any): FindFluent[T] = copy(filter)

  def limit(limit: Int): FindFluent[T] = copy(findOptions.limit(limit))

  def skip(skip: Int): FindFluent[T] = copy(findOptions.skip(skip))

  def maxTime(duration: Duration): FindFluent[T] = copy(findOptions.maxTime(duration.toMillis, MILLISECONDS))

  def batchSize(batchSize: Int): FindFluent[T] = copy(findOptions.batchSize(batchSize))

  def modifiers(modifiers: Any): FindFluent[T] = copy(findOptions.modifiers(modifiers))

  def projection(projection: Any): FindFluent[T] = copy(findOptions.projection(projection))

  def sort(sort: Any): FindFluent[T] = copy(findOptions.sort(sort))

  def awaitData(awaitData: Boolean): FindFluent[T] = copy(findOptions.awaitData(awaitData))

  def noCursorTimeout(noCursorTimeout: Boolean): FindFluent[T] = copy(findOptions.noCursorTimeout(noCursorTimeout))

  def oplogReplay(oplogReplay: Boolean): FindFluent[T] = copy(findOptions.oplogReplay(oplogReplay))

  def partial(partial: Boolean): FindFluent[T] = copy(findOptions.partial(partial))

  def tailable(tailable: Boolean): FindFluent[T] = copy(findOptions.tailable(tailable))

  override def first(): ResultType[Option[T]] =
    execute(createQueryOperation.batchSize(0).limit(-1)).first().asInstanceOf[ResultType[Option[T]]]

  override def forEach(block: T => Unit): ResultType[Unit] =
    execute(createQueryOperation).forEach(block).asInstanceOf[ResultType[Unit]]

  override def into(target: mutable.Buffer[T]): ResultType[Unit] =
    execute(createQueryOperation).into(target).asInstanceOf[ResultType[Unit]]

  private def execute(operation: FindOperation[T]): MongoIterable[T] =
    operationIterable[T](operation, options.getReadPreference, executor, clazz)

  private def getCodec: Codec[T] = options.getCodecRegistry.get(clazz)

  private def createQueryOperation: FindOperation[T] =
    new FindOperation[T](namespace, getCodec).filter(asBson(filter)).batchSize(findOptions.getBatchSize)
      .skip(findOptions.getSkip).limit(findOptions.getLimit)
      .maxTime(findOptions.getMaxTime(MILLISECONDS), MILLISECONDS).modifiers(asBson(findOptions.getModifiers))
      .projection(asBson(findOptions.getProjection)).sort(asBson(findOptions.getSort))
      .awaitData(findOptions.isAwaitData).noCursorTimeout(findOptions.isNoCursorTimeout)
      .oplogReplay(findOptions.isOplogReplay).partial(findOptions.isPartial).tailableCursor(findOptions.isTailable)
      .slaveOk(options.getReadPreference.isSlaveOk)

  private def asBson(document: Any): BsonDocument =
    BsonDocumentWrapper.asBsonDocument(document, options.getCodecRegistry)

  private def copy(filter: Any): FindFluent[T] = copy(namespace, filter, findOptions, options, executor, clazz)

  private def copy(findOptions: FindOptions): FindFluent[T] =
    copy(namespace, filter, findOptions, options, executor, clazz)

}
