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

  /**
   * The namespace of the collection
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val namespace: MongoNamespace

  /**
   * The filter to be applied to the collection
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val filter: Any

  /**
   * The options to apply to a find operation (also commonly referred to as a query).
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val findOptions: FindOptions

  /**
   * The various settings to control the behavior of Operations when they are executed.
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val options: OperationOptions

  /**
   * The AsyncOperationExecutor to be used with this MongoCollectionFindFluent instance
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val executor: AsyncOperationExecutor

  /**
   * The class to decode each document into
   *
   * @note Its expected that the MongoCollectionFindFluent implementation is a case class and this is one of the
   *       constructor params. This is passed in from the MongoCollection Implementation
   */
  val clazz: Class[T]

  protected def copy(namespace: MongoNamespace, filter: Any, findOptions: FindOptions, options: OperationOptions,
                     executor: AsyncOperationExecutor, clazz: Class[T]): FindFluent[T]

  /**
   * Sets the query filter to apply to the query.
   *
   * @param filter the filter, which may be null.
   * @return a new instance of FindFluent
   */
  def filter(filter: Any): FindFluent[T] = copy(filter)

  /**
   * Sets the limit to apply.
   *
   * @param limit the limit
   * @return a new instance of FindFluent
   */
  def limit(limit: Int): FindFluent[T] = copy(findOptions.limit(limit))

  /**
   * Sets the skip to apply.
   *
   * @param skip the skip
   * @return a new instance of FindFluent
   */
  def skip(skip: Int): FindFluent[T] = copy(findOptions.skip(skip))

  /**
   * Sets the maximum execution time on the server for this operation.
   *
   * @param duration  the duration
   * @return a new instance of FindFluent
   */
  def maxTime(duration: Duration): FindFluent[T] = copy(findOptions.maxTime(duration.toMillis, MILLISECONDS))

  /**
   * Sets the number of documents to return per batch.
   *
   * @param batchSize the batch size
   * @return a new instance of FindFluent
   */
  def batchSize(batchSize: Int): FindFluent[T] = copy(findOptions.batchSize(batchSize))

  /**
   * Sets the query modifiers to apply to this operation.
   *
   * @param modifiers the query modifiers to apply, which may be null.
   * @return a new instance of FindFluent
   */
  def modifiers(modifiers: Any): FindFluent[T] = copy(findOptions.modifiers(modifiers))

  /**
   * Sets a document describing the fields to return for all matching documents.
   *
   * @param projection the project document
   * @return a new instance of FindFluent
   */
  def projection(projection: Any): FindFluent[T] = copy(findOptions.projection(projection))

  /**
   * Sets the sort criteria to apply to the query.
   *
   * @param sort the sort criteria
   * @return a new instance of FindFluent
   */
  def sort(sort: Any): FindFluent[T] = copy(findOptions.sort(sort))

  /**
   * Use with the tailable property. If there are no more matching documents, the server will block for a
   * while rather than returning no documents.
   *
   * @param awaitData whether the cursor will wait for more documents that match the filter
   * @return a new instance of FindFluent
   */
  def awaitData(awaitData: Boolean): FindFluent[T] = copy(findOptions.awaitData(awaitData))

  /**
   * The server normally times out idle cursors after an inactivity period (10 minutes)
   * to prevent excess memory use. Set this option to prevent that.
   *
   * @param noCursorTimeout true if cursor timeout is disabled
   * @return a new instance of FindFluent
   */
  def noCursorTimeout(noCursorTimeout: Boolean): FindFluent[T] = copy(findOptions.noCursorTimeout(noCursorTimeout))

  /**
   * Users should not set this under normal circumstances.
   *
   * @param oplogReplay if oplog replay is enabled
   * @return a new instance of FindFluent
   */
  def oplogReplay(oplogReplay: Boolean): FindFluent[T] = copy(findOptions.oplogReplay(oplogReplay))

  /**
   * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
   *
   * @param partial if partial results for sharded clusters is enabled
   * @return a new instance of FindFluent
   */
  def partial(partial: Boolean): FindFluent[T] = copy(findOptions.partial(partial))

  /**
   * Tailable means the cursor is not closed when the last data is retrieved.
   * Rather, the cursor marks the documents's position. You can resume
   * using the cursor later, from where it was located, if more data were
   * received. Like any "latent cursor", the cursor may become invalid at
   * some point - for example if the document it references is deleted.
   *
   * @param tailable if tailable is enabled
   * @return a new instance of FindFluent
   */
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
