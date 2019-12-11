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

import java.nio.ByteBuffer

import org.bson.BsonValue
import org.mongodb.scala.{Completed, Observable, Observer, SingleObservable, Subscription}
import org.mongodb.scala.bson.ObjectId

/**
 * A GridFS `Observable` for uploading data into GridFS
 *
 * Provides the `id` for the file to be uploaded. Cancelling the subscription to this publisher will cause any uploaded data
 * to be cleaned up and removed.
 *
 * @tparam T the result type of the publisher
 * @since 2.8
 */
trait GridFSUploadObservable[T] extends SingleObservable[T] {
  /**
   * Gets the ObjectId for the file to be uploaded
   *
   * @throws MongoGridFSException if the file id is not an ObjectId.
   *
   * @return the ObjectId for the file to be uploaded
   */
  def objectId: ObjectId

  /**
   * The BsonValue id for this file.
   *
   * @return the id for this file
   */
  def id: BsonValue
}

private[gridfs] case class GridFSUploadObservableImpl(
    gridFSUploadStream: GridFSUploadStream,
    source:             Observable[ByteBuffer]
) extends GridFSUploadObservable[Completed] {

  override def objectId: ObjectId = gridFSUploadStream.objectId

  override def id: BsonValue = gridFSUploadStream.id

  override def subscribe(observer: Observer[_ >: Completed]): Unit =
    observer.onSubscribe(GridFSUploadSubscription(observer))

  private case class GridFSUploadSubscription(outerObserver: Observer[_ >: Completed]) extends Subscription {
    /* protected by `this` */
    private var requested: Long = 0
    private var hasCompleted = false
    private var currentAction = Action.WAITING
    private var sourceSubscription: Option[Subscription] = None
    private var unsubscribed = false
    /* protected by `this` */

    private def inLock(func: () => Unit): Unit = {
      this.synchronized {
        func()
      }
    }

    private val sourceObserver = new Observer[ByteBuffer]() {
      override def onSubscribe(s: Subscription): Unit = {
        inLock { () =>
          sourceSubscription = Some(s)
          currentAction = Action.WAITING
        }
        tryProcess()
      }

      override def onNext(byteBuffer: ByteBuffer): Unit = {
        gridFSUploadStream.write(byteBuffer).subscribe(GridFSUploadStreamObserver(byteBuffer))
      }

      override def onError(t: Throwable): Unit = {
        inLock { () => currentAction = Action.FINISHED }
        outerObserver.onError(t)
      }

      override def onComplete(): Unit = {
        inLock { () =>
          hasCompleted = true
          if (currentAction eq Action.WAITING) {
            currentAction = Action.COMPLETE
          }
        }
      }
    }

    private case class GridFSUploadStreamObserver(byteBuffer: ByteBuffer) extends Observer[Int] {
      override def onSubscribe(s: Subscription): Unit = {
        s.request(1)
      }

      override def onNext(integer: Int): Unit = {}

      override def onError(t: Throwable): Unit = {
        terminate()
        outerObserver.onError(t)
      }

      override def onComplete(): Unit = {
        if (byteBuffer.remaining > 0) {
          sourceObserver.onNext(byteBuffer)
        } else {
          inLock { () =>
            if (hasCompleted) {
              currentAction = Action.COMPLETE
            }
            if (unsubscribed) {
              currentAction = Action.TERMINATE
            }
            if ((currentAction ne Action.COMPLETE) && (currentAction ne Action.TERMINATE)
              && (currentAction ne Action.FINISHED)) {
              currentAction = Action.WAITING
            }
          }
          tryProcess()
        }
      }
    }

    override def request(n: Long): Unit = {
      inLock { () => requested += n }
      tryProcess()
    }

    override def unsubscribe(): Unit = {
      inLock { () => unsubscribed = true }
      terminate()
    }

    override def isUnsubscribed: Boolean = synchronized(unsubscribed)

    // scalastyle:off cyclomatic.complexity method.length
    private def tryProcess(): Unit = {
      var nextStep: Option[NextStep.Value] = None
      inLock { () =>
        currentAction match {
          case Action.WAITING =>
            if (requested == 0) {
              nextStep = Some(NextStep.DO_NOTHING)
            } else if (sourceSubscription.isEmpty) {
              nextStep = Some(NextStep.SUBSCRIBE)
              currentAction = Action.IN_PROGRESS
            } else {
              nextStep = Some(NextStep.WRITE)
              currentAction = Action.IN_PROGRESS
            }
          case Action.COMPLETE =>
            nextStep = Some(NextStep.COMPLETE)
            currentAction = Action.FINISHED
          case Action.TERMINATE =>
            nextStep = Some(NextStep.TERMINATE)
            currentAction = Action.FINISHED
          case _ =>
            nextStep = Some(NextStep.DO_NOTHING)
        }
      }

      nextStep.get match {
        case NextStep.SUBSCRIBE =>
          source.subscribe(sourceObserver)
        case NextStep.WRITE =>
          inLock { () => sourceSubscription.get.request(1) }
        case NextStep.COMPLETE =>
          gridFSUploadStream.close().subscribe(new Observer[Completed]() {
            override def onSubscribe(s: Subscription): Unit = {
              inLock { () => requested -= 1 }
              s.request(1)
            }

            override def onNext(result: Completed): Unit = {
              outerObserver.onNext(result)
            }

            override def onError(t: Throwable): Unit = {
              outerObserver.onError(t)
            }

            override def onComplete(): Unit = {
              outerObserver.onComplete()
            }
          })
        case NextStep.TERMINATE =>
          gridFSUploadStream.abort().subscribe(new Observer[Completed]() {
            override def onSubscribe(s: Subscription): Unit = {
              inLock { () => requested -= 1 }
              s.request(1)
            }

            override def onNext(success: Completed): Unit = {
            }

            override def onError(t: Throwable): Unit = {
            }

            override def onComplete(): Unit = {
            }
          })
        case _ =>
        // Do nothing
      }
    }
    // scalastyle:on cyclomatic.complexity method.length

    private def terminate(): Unit = {
      inLock { () => currentAction = Action.TERMINATE }
      tryProcess()
    }
  }

  def withObjectId(): GridFSUploadObservable[ObjectId] = {
    val wrapped: GridFSUploadObservableImpl = this
    new GridFSUploadObservable[ObjectId] {
      override def objectId: ObjectId = wrapped.objectId

      override def id: BsonValue = wrapped.id

      override def subscribe(observer: Observer[_ >: ObjectId]): Unit = {
        wrapped.subscribe(
          new Observer[Completed] {

            override def onSubscribe(s: Subscription): Unit = observer.onSubscribe(s)

            override def onNext(result: Completed): Unit = observer.onNext(objectId)

            override def onError(e: Throwable): Unit = observer.onError(e)

            override def onComplete(): Unit = observer.onComplete()
          }
        )
      }
    }
  }

  private object Action extends Enumeration {
    type Action = Value
    val WAITING, IN_PROGRESS, TERMINATE, COMPLETE, FINISHED = Value
  }

  private object NextStep extends Enumeration {
    type NextStep = Value
    val SUBSCRIBE, WRITE, COMPLETE, TERMINATE, DO_NOTHING = Value
  }
}
