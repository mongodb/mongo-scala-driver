package org.mongodb.scala.core

import com.mongodb.ReadPreference
import com.mongodb.operation.{ AsyncOperationExecutor, AsyncWriteOperation, AsyncReadOperation }

trait ExecutorHelper {

  protected val executor: AsyncOperationExecutor

  private[scala] def executeAsync[T, R, F](operation: AsyncReadOperation[T], readPreference: ReadPreference,
                                           callback: ResultCallback[T, R, F]): F = {
    executor.execute(operation, readPreference, callback)
    callback.result
  }

  private[scala] def executeAsync[T, R, F](operation: AsyncWriteOperation[T], callback: ResultCallback[T, R, F]): F = {
    executor.execute(operation, callback)
    callback.result
  }

}
