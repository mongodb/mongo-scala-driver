/*
 * Copyright 2016 MongoDB, Inc.
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

import org.bson.types.ObjectId
import com.mongodb.async.client.gridfs.{GridFSBucket => JGridFSBucket}
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{ClientSession, ReadConcern, ReadPreference, WriteConcern}
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class GridFSBucketSpec extends FlatSpec with Matchers with MockFactory {
  val wrapper = mock[JGridFSBucket]
  val clientSession = mock[ClientSession]
  val gridFSBucket = new GridFSBucket(wrapper)

  "GridFSBucket" should "have the same methods as the wrapped GridFSBucket" in {
    val wrapped = classOf[JGridFSBucket].getMethods.map(_.getName).toSet
    val local = classOf[GridFSBucket].getMethods.map(_.getName).toSet

    wrapped.foreach((name: String) => {
      val cleanedName = name.stripPrefix("get")
      assert(local.contains(name) | local.contains(cleanedName.head.toLower + cleanedName.tail))
    })
  }

  it should "call the underlying methods to get bucket values" in {
    wrapper.expects('getBucketName)().once()
    wrapper.expects('getDisableMD5)().returning(false).once()
    wrapper.expects('getChunkSizeBytes)().returning(1).once()
    wrapper.expects('getReadConcern)().once()
    wrapper.expects('getReadPreference)().once()
    wrapper.expects('getWriteConcern)().once()

    gridFSBucket.bucketName
    gridFSBucket.disableMD5
    gridFSBucket.chunkSizeBytes
    gridFSBucket.readConcern
    gridFSBucket.readPreference
    gridFSBucket.writeConcern
  }

  it should "call the underlying methods to set bucket values" in {
    val chunkSizeInBytes = 1024 * 1024
    val readConcern = ReadConcern.MAJORITY
    val readPreference = ReadPreference.secondaryPreferred()
    val writeConcern = WriteConcern.W2

    wrapper.expects('withDisableMD5)(true).once()
    wrapper.expects('withChunkSizeBytes)(chunkSizeInBytes).once()
    wrapper.expects('withReadConcern)(readConcern).once()
    wrapper.expects('withReadPreference)(readPreference).once()
    wrapper.expects('withWriteConcern)(writeConcern).once()

    gridFSBucket.withDisableMD5(true)
    gridFSBucket.withChunkSizeBytes(chunkSizeInBytes)
    gridFSBucket.withReadConcern(readConcern)
    gridFSBucket.withReadPreference(readPreference)
    gridFSBucket.withWriteConcern(writeConcern)
  }

  it should "call the underlying delete method" in {
    val objectId = new ObjectId()
    val bsonValue = new BsonObjectId(objectId)

    wrapper.expects('delete)(objectId, *).once()
    wrapper.expects('delete)(bsonValue, *).once()
    wrapper.expects('delete)(clientSession, objectId, *).once()
    wrapper.expects('delete)(clientSession, bsonValue, *).once()

    gridFSBucket.delete(objectId).head()
    gridFSBucket.delete(bsonValue).head()
    gridFSBucket.delete(clientSession, objectId).head()
    gridFSBucket.delete(clientSession, bsonValue).head()
  }

  it should "call the underlying drop method" in {
    wrapper.expects('drop)(*).once()
    wrapper.expects('drop)(clientSession, *).once()

    gridFSBucket.drop().head()
    gridFSBucket.drop(clientSession).head()
  }

  it should "call the underlying rename method" in {
    val objectId = new ObjectId()
    val bsonValue = new BsonObjectId(objectId)
    val newName = "newName"

    wrapper.expects('rename)(objectId, newName, *).once()
    wrapper.expects('rename)(bsonValue, newName, *).once()
    wrapper.expects('rename)(clientSession, objectId, newName, *).once()
    wrapper.expects('rename)(clientSession, bsonValue, newName, *).once()

    gridFSBucket.rename(objectId, newName).head()
    gridFSBucket.rename(bsonValue, newName).head()
    gridFSBucket.rename(clientSession, objectId, newName).head()
    gridFSBucket.rename(clientSession, bsonValue, newName).head()
  }

  it should "return the expected findObservable" in {
    val filter = Document("{a: 1}")

    wrapper.expects('find)().once()
    wrapper.expects('find)(filter).once()
    wrapper.expects('find)(clientSession).once()
    wrapper.expects('find)(clientSession, filter).once()

    gridFSBucket.find().head()
    gridFSBucket.find(filter).head()
    gridFSBucket.find(clientSession).head()
    gridFSBucket.find(clientSession, filter).head()
  }

  it should "create the expected GridFSDownloadStream" in {
    val filename = "fileName"
    val options = new GridFSDownloadOptions()
    val objectId = new ObjectId()
    val bsonValue = new BsonObjectId(objectId)

    wrapper.expects('openDownloadStream)(filename)
    wrapper.expects('openDownloadStream)(filename, options)
    wrapper.expects('openDownloadStream)(objectId)
    wrapper.expects('openDownloadStream)(bsonValue)
    wrapper.expects('openDownloadStream)(clientSession, filename)
    wrapper.expects('openDownloadStream)(clientSession, filename, options)
    wrapper.expects('openDownloadStream)(clientSession, objectId)
    wrapper.expects('openDownloadStream)(clientSession, bsonValue)

    gridFSBucket.openDownloadStream(filename)
    gridFSBucket.openDownloadStream(filename, options)
    gridFSBucket.openDownloadStream(objectId)
    gridFSBucket.openDownloadStream(bsonValue)
    gridFSBucket.openDownloadStream(clientSession, filename)
    gridFSBucket.openDownloadStream(clientSession, filename, options)
    gridFSBucket.openDownloadStream(clientSession, objectId)
    gridFSBucket.openDownloadStream(clientSession, bsonValue)
  }

  it should "downloadToStream as expected" in {
    val filename = "fileName"
    val options = new GridFSDownloadOptions()
    val objectId = new ObjectId()
    val bsonValue = new BsonObjectId(objectId)
    val outputStream = mock[AsyncOutputStream]

    wrapper.expects('downloadToStream)(filename, *, *)
    wrapper.expects('downloadToStream)(filename, *, options, *)
    wrapper.expects('downloadToStream)(objectId, *, *)
    wrapper.expects('downloadToStream)(bsonValue, *, *)
    wrapper.expects('downloadToStream)(clientSession, filename, *, *)
    wrapper.expects('downloadToStream)(clientSession, filename, *, options, *)
    wrapper.expects('downloadToStream)(clientSession, objectId, *, *)
    wrapper.expects('downloadToStream)(clientSession, bsonValue, *, *)

    gridFSBucket.downloadToStream(filename, outputStream).head()
    gridFSBucket.downloadToStream(filename, outputStream, options).head()
    gridFSBucket.downloadToStream(objectId, outputStream).head()
    gridFSBucket.downloadToStream(bsonValue, outputStream).head()
    gridFSBucket.downloadToStream(clientSession, filename, outputStream).head()
    gridFSBucket.downloadToStream(clientSession, filename, outputStream, options).head()
    gridFSBucket.downloadToStream(clientSession, objectId, outputStream).head()
    gridFSBucket.downloadToStream(clientSession, bsonValue, outputStream).head()
  }

  it should "create the expected GridFSUploadStream" in {
    val filename = "fileName"
    val options = new GridFSUploadOptions()
    val bsonValue = new BsonObjectId()

    wrapper.expects('openUploadStream)(filename)
    wrapper.expects('openUploadStream)(filename, options)
    wrapper.expects('openUploadStream)(bsonValue, filename)
    wrapper.expects('openUploadStream)(bsonValue, filename, options)
    wrapper.expects('openUploadStream)(clientSession, filename)
    wrapper.expects('openUploadStream)(clientSession, filename, options)
    wrapper.expects('openUploadStream)(clientSession, bsonValue, filename)
    wrapper.expects('openUploadStream)(clientSession, bsonValue, filename, options)

    gridFSBucket.openUploadStream(filename)
    gridFSBucket.openUploadStream(filename, options)
    gridFSBucket.openUploadStream(bsonValue, filename)
    gridFSBucket.openUploadStream(bsonValue, filename, options)
    gridFSBucket.openUploadStream(clientSession, filename)
    gridFSBucket.openUploadStream(clientSession, filename, options)
    gridFSBucket.openUploadStream(clientSession, bsonValue, filename)
    gridFSBucket.openUploadStream(clientSession, bsonValue, filename, options)
  }

  it should "uploadFromStream as expected" in {
    val filename = "fileName"
    val options = new GridFSUploadOptions()
    val bsonValue = new BsonObjectId()
    val inputStream = mock[AsyncInputStream]

    wrapper.expects('uploadFromStream)(filename, *, *)
    wrapper.expects('uploadFromStream)(filename, *, options, *)
    wrapper.expects('uploadFromStream)(bsonValue, filename, *, *)
    wrapper.expects('uploadFromStream)(bsonValue, filename, *, options, *)
    wrapper.expects('uploadFromStream)(clientSession, filename, *, *)
    wrapper.expects('uploadFromStream)(clientSession, filename, *, options, *)
    wrapper.expects('uploadFromStream)(clientSession, bsonValue, filename, *, *)
    wrapper.expects('uploadFromStream)(clientSession, bsonValue, filename, *, options, *)

    gridFSBucket.uploadFromStream(filename, inputStream).head()
    gridFSBucket.uploadFromStream(filename, inputStream, options).head()
    gridFSBucket.uploadFromStream(bsonValue, filename, inputStream).head()
    gridFSBucket.uploadFromStream(bsonValue, filename, inputStream, options).head()
    gridFSBucket.uploadFromStream(clientSession, filename, inputStream).head()
    gridFSBucket.uploadFromStream(clientSession, filename, inputStream, options).head()
    gridFSBucket.uploadFromStream(clientSession, bsonValue, filename, inputStream).head()
    gridFSBucket.uploadFromStream(clientSession, bsonValue, filename, inputStream, options).head()
  }

}
