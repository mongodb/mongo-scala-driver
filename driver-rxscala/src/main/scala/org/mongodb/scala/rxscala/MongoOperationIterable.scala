package org.mongodb.scala.rxscala
import com.mongodb.ReadPreference
import com.mongodb.operation.{AsyncBatchCursor, AsyncReadOperation, AsyncOperationExecutor}
import org.mongodb.scala.core.MongoOperationIterableProvider

case class MongoOperationIterable[T](operation: AsyncReadOperation[AsyncBatchCursor[T]], readPreference: ReadPreference,
                                     executor: AsyncOperationExecutor, clazz: Class[T]) extends
MongoOperationIterableProvider[T] with RequiredTypesAndTransformers {

}
