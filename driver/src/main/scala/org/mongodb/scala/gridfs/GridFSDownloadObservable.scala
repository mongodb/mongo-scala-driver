/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.scala.gridfs

import java.nio.{Buffer, ByteBuffer}

import org.mongodb.scala.{Completed, Observable, Observer, Subscription}

case class GridFSDownloadObservable(gridFSDownloadStream: GridFSDownloadStream) extends Observable[ByteBuffer] {
  private var bufferSizeBytes: Int = 0

  def getGridFSFile: Observable[GridFSFile] = gridFSDownloadStream.gridFSFile()

  def bufferSizeBytes(bufferSizeBytes: Int): GridFSDownloadObservable = {
    this.bufferSizeBytes = bufferSizeBytes
    this
  }

  override def subscribe(observer: Observer[_ >: ByteBuffer]): Unit = {
    observer.onSubscribe(GridFSDownloadSubscription(observer))
  }

  private case class GridFSDownloadSubscription(outerObserver: Observer[_ >: ByteBuffer]) extends Subscription {

    /* protected by `this` */
    private var gridFSFile: Option[GridFSFile] = None
    private var sizeRead = 0
    private var requested: Long = 0
    private var currentBatchSize = 0
    private var currentAction: Action.Value = Action.WAITING
    private var unsubscribed = false
    /* protected by `this` */

    private def inLock(func: () => Unit): Unit = {
      this.synchronized {
        func()
      }
    }

    final private val gridFSFileObserver = new Observer[GridFSFile]() {
      override def onSubscribe(s: Subscription): Unit = {
        s.request(1)
      }

      override def onNext(result: GridFSFile): Unit = {
        inLock { () => gridFSFile = Some(result) }
      }

      override def onError(t: Throwable): Unit = {
        terminate()
        outerObserver.onError(t)
      }

      override def onComplete(): Unit = {
        inLock { () => currentAction = Action.WAITING }
        tryProcess()
      }
    }

    private case class GridFSDownloadStreamObserver(byteBuffer: ByteBuffer) extends Observer[Int] {
      override def onSubscribe(s: Subscription): Unit = {
        s.request(1)
      }

      override def onNext(integer: Int): Unit = {
        inLock { () => sizeRead += integer }
      }

      override def onError(t: Throwable): Unit = {
        terminate()
        outerObserver.onError(t)
      }

      override def onComplete(): Unit = {
        if (byteBuffer.remaining > 0) {
          gridFSDownloadStream.read(byteBuffer).subscribe(GridFSDownloadStreamObserver(byteBuffer))
        } else {
          var hasTerminated = false
          inLock { () =>
            hasTerminated = (currentAction eq Action.TERMINATE) || (currentAction eq Action.FINISHED)
            if (!hasTerminated) {
              requested -= 1
              currentAction = Action.WAITING
              if (gridFSFile.get.getLength.toInt == sizeRead) {
                currentAction = Action.COMPLETE
              }
            }
          }

          if (!hasTerminated) {
            byteBuffer.asInstanceOf[Buffer].flip
            outerObserver.onNext(byteBuffer)
            tryProcess()
          }
        }
      }
    }

    override def request(n: Long): Unit = {
      inLock { () =>
        if (!isUnsubscribed && n < 1) {
          currentAction = Action.FINISHED
        } else {
          requested += n
        }
      }
      tryProcess()
    }

    // scalastyle:off cyclomatic.complexity method.length
    private def tryProcess(): Unit = {
      var nextStep: Option[NextStep.Value] = None
      inLock { () =>
        currentAction match {
          case Action.WAITING => {
            if (requested == 0) {
              nextStep = Some(NextStep.DO_NOTHING)
            } else if (gridFSFile.isEmpty) {
              nextStep = Some(NextStep.GET_FILE)
              currentAction = Action.IN_PROGRESS
            } else if (sizeRead == gridFSFile.get.getLength) {
              nextStep = Some(NextStep.COMPLETE)
              currentAction = Action.FINISHED
            } else {
              nextStep = Some(NextStep.READ)
              currentAction = Action.IN_PROGRESS
            }
          }
          case Action.COMPLETE =>
            nextStep = Some(NextStep.COMPLETE)
            currentAction = Action.FINISHED
          case Action.TERMINATE =>
            currentAction = Action.FINISHED
            nextStep = Some(NextStep.TERMINATE)
          case _ =>
            nextStep = Some(NextStep.DO_NOTHING)
        }
      }

      nextStep.get match {
        case NextStep.GET_FILE =>
          getGridFSFile.subscribe(gridFSFileObserver)

        case NextStep.READ =>
          var chunkSize = 0
          var remaining = 0L
          inLock { () =>
            chunkSize = gridFSFile.get.getChunkSize
            remaining = gridFSFile.get.getLength - sizeRead
          }

          var byteBufferSize = Math.max(chunkSize, bufferSizeBytes)
          if (remaining < Integer.MAX_VALUE) {
            byteBufferSize = Math.min(remaining.intValue, byteBufferSize)
          }
          val byteBuffer = ByteBuffer.allocate(byteBufferSize)
          if (currentBatchSize == 0) {
            currentBatchSize = Math.max(byteBufferSize / chunkSize, 1)
            gridFSDownloadStream.batchSize(currentBatchSize)
          }
          gridFSDownloadStream.read(byteBuffer).subscribe(GridFSDownloadStreamObserver(byteBuffer))

        case NextStep.COMPLETE | NextStep.TERMINATE =>
          val propagateToOuter = nextStep.get eq NextStep.COMPLETE
          gridFSDownloadStream.close().subscribe(new Observer[Completed]() {
            override def onSubscribe(s: Subscription): Unit = {
              s.request(1)
            }

            override def onNext(success: Completed): Unit = {
            }

            override def onError(t: Throwable): Unit = {
              if (propagateToOuter) outerObserver.onError(t)
            }

            override def onComplete(): Unit = {
              if (propagateToOuter) outerObserver.onComplete()
            }
          })
        case _ =>
      }
    }

    // scalastyle:on cyclomatic.complexity method.length

    override def unsubscribe(): Unit = {
      inLock { () => unsubscribed = true }
      terminate()
    }

    override def isUnsubscribed: Boolean = synchronized(unsubscribed)

    private def terminate(): Unit = {
      inLock { () => currentAction = Action.TERMINATE }
      tryProcess()
    }

  }

  private object Action extends Enumeration {
    type Action = Value
    val WAITING, IN_PROGRESS, TERMINATE, COMPLETE, FINISHED = Value
  }

  private object NextStep extends Enumeration {
    type NextStep = Value
    val GET_FILE, READ, COMPLETE, TERMINATE, DO_NOTHING = Value
  }

}
