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
import com.mongodb.async.client.gridfs.{ GridFSBucket => JGridFSBucket }

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{ ReadConcern, ReadPreference, WriteConcern }
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.{ FlatSpec, Matchers }

class GridFSBucketSpec extends FlatSpec with Matchers with MockFactory {
  val wrapper = mock[JGridFSBucket]
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
    wrapper.expects('getChunkSizeBytes)().returning(1).once()
    wrapper.expects('getReadConcern)().once()
    wrapper.expects('getReadPreference)().once()
    wrapper.expects('getWriteConcern)().once()

    gridFSBucket.bucketName
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

    wrapper.expects('withChunkSizeBytes)(chunkSizeInBytes).once()
    wrapper.expects('withReadConcern)(readConcern).once()
    wrapper.expects('withReadPreference)(readPreference).once()
    wrapper.expects('withWriteConcern)(writeConcern).once()

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

    gridFSBucket.delete(objectId).head()
    gridFSBucket.delete(bsonValue).head()
  }

  it should "call the underlying drop method" in {
    wrapper.expects('drop)(*).once()

    gridFSBucket.drop().head()
  }

  it should "call the underlying rename method" in {
    val objectId = new ObjectId()
    val bsonValue = new BsonObjectId(objectId)
    val newName = "newName"

    wrapper.expects('rename)(objectId, newName, *).once()
    wrapper.expects('rename)(bsonValue, newName, *).once()

    gridFSBucket.rename(objectId, newName).head()
    gridFSBucket.rename(bsonValue, newName).head()
  }

  it should "return the expected findObservable" in {
    val filter = Document("{a: 1}")

    wrapper.expects('find)().once()
    wrapper.expects('find)(filter).once()

    gridFSBucket.find().head()
    gridFSBucket.find(filter).head()
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

    gridFSBucket.openDownloadStream(filename)
    gridFSBucket.openDownloadStream(filename, options)
    gridFSBucket.openDownloadStream(objectId)
    gridFSBucket.openDownloadStream(bsonValue)
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

    gridFSBucket.downloadToStream(filename, outputStream).head()
    gridFSBucket.downloadToStream(filename, outputStream, options).head()
    gridFSBucket.downloadToStream(objectId, outputStream).head()
    gridFSBucket.downloadToStream(bsonValue, outputStream).head()
  }

  it should "create the expected GridFSUploadStream" in {
    val filename = "fileName"
    val options = new GridFSUploadOptions()
    val bsonValue = new BsonObjectId()

    wrapper.expects('openUploadStream)(filename)
    wrapper.expects('openUploadStream)(filename, options)
    wrapper.expects('openUploadStream)(bsonValue, filename)
    wrapper.expects('openUploadStream)(bsonValue, filename, options)

    gridFSBucket.openUploadStream(filename)
    gridFSBucket.openUploadStream(filename, options)
    gridFSBucket.openUploadStream(bsonValue, filename)
    gridFSBucket.openUploadStream(bsonValue, filename, options)
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

    gridFSBucket.uploadFromStream(filename, inputStream).head()
    gridFSBucket.uploadFromStream(filename, inputStream, options).head()
    gridFSBucket.uploadFromStream(bsonValue, filename, inputStream).head()
    gridFSBucket.uploadFromStream(bsonValue, filename, inputStream, options).head()
  }

}
