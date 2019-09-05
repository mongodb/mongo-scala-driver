/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala

import java.nio.ByteBuffer

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.{AsyncInputStream => JAsyncInputStream, AsyncOutputStream => JAsyncOutputStream}
import org.mongodb.scala.internal.ObservableHelper.{observeCompleted, observeInt, observeLong}

package object gridfs {

  /**
   * An exception indicating that a failure occurred in GridFS.
   */
  type MongoGridFSException = com.mongodb.MongoGridFSException

  /**
   * GridFS upload options
   *
   * Customizable options used when uploading files into GridFS
   */
  type GridFSUploadOptions = com.mongodb.client.gridfs.model.GridFSUploadOptions

  /**
   * The GridFSFile
   */
  type GridFSFile = com.mongodb.client.gridfs.model.GridFSFile
  /**
   * The GridFS download by name options
   *
   * Controls the selection of the revision to download
   */
  type GridFSDownloadOptions = com.mongodb.client.gridfs.model.GridFSDownloadOptions

  implicit class JavaAsyncInputStreamToScala(wrapped: JAsyncInputStream) extends AsyncInputStream {

    override def close(): Observable[Completed] = observeCompleted(wrapped.close(_: SingleResultCallback[Void]))

    override def read(dst: ByteBuffer): Observable[Int] = observeInt(wrapped.read(dst, _: SingleResultCallback[java.lang.Integer]))

    override def skip(bytesToSkip: Long): Observable[Long] = observeLong(wrapped.skip(bytesToSkip, _: SingleResultCallback[java.lang.Long]))
  }

  implicit class JavaAsyncOutputStreamToScala(wrapped: JAsyncOutputStream) extends AsyncOutputStream {

    override def close(): Observable[Completed] = observeCompleted(wrapped.close(_: SingleResultCallback[Void]))

    override def write(src: ByteBuffer): Observable[Int] = observeInt(wrapped.write(src, _: SingleResultCallback[java.lang.Integer]))
  }

  implicit class ScalaAsyncInputStreamToJava(wrapped: AsyncInputStream) extends JAsyncInputStream {
    // scalastyle:off null
    override def close(callback: SingleResultCallback[Void]): Unit = wrapped.close().subscribe(
      (_: Completed) => (),
      (e: Throwable) => callback.onResult(null, e),
      () => callback.onResult(null, null)
    )

    override def read(dst: ByteBuffer, callback: SingleResultCallback[Integer]): Unit = wrapped.read(dst).subscribe(
      new Observer[Int] {
        var bytesRead: Option[Int] = None
        override def onError(e: Throwable): Unit = callback.onResult(null, e)

        override def onComplete(): Unit = bytesRead.foreach(callback.onResult(_, null))

        override def onNext(result: Int): Unit = bytesRead = Some(result)
      }
    )
    override def skip(bytesToSkip: Long, callback: SingleResultCallback[java.lang.Long]): Unit = wrapped.skip(bytesToSkip).subscribe(
      new Observer[Long] {
        var bytesSkipped: Option[Long] = None
        override def onError(e: Throwable): Unit = callback.onResult(null, e)
        override def onComplete(): Unit = callback.onResult(bytesSkipped.getOrElse(0L).asInstanceOf[java.lang.Long], null)
        override def onNext(result: Long): Unit = bytesSkipped = Some(result)
      }
    )
    // scalastyle:on null
  }

  implicit class ScalaAsyncOutputStreamToJava(wrapped: AsyncOutputStream) extends JAsyncOutputStream {
    // scalastyle:off null
    override def write(src: ByteBuffer, callback: SingleResultCallback[Integer]): Unit = wrapped.write(src).subscribe(
      new Observer[Int] {
        var bytesWritten: Option[Int] = None

        override def onError(e: Throwable): Unit = callback.onResult(null, e)

        override def onComplete(): Unit = bytesWritten.foreach(callback.onResult(_, null))

        override def onNext(result: Int): Unit = bytesWritten = Some(result)
      }
    )

    override def close(callback: SingleResultCallback[Void]): Unit = wrapped.close().subscribe(
      (_: Completed) => (),
      (e: Throwable) => callback.onResult(null, e),
      () => callback.onResult(null, null)
    )

    // scalastyle:off null
  }
}
