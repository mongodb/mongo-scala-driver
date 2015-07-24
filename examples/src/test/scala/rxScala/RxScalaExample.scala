/*
 * Copyright 2015 MongoDB, Inc.
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

package rxScala

import scala.collection.immutable.IndexedSeq

import org.mongodb.scala._
import rx.lang.{scala => rx}
import rxScala.Implicits._

/**
 * The RxScala Example usage
 */
object RxScalaExample {

  /**
   * Run this main method to see the output of this quick example.
   *
   * @param args takes an optional single argument for the connection string
   * @throws Throwable if an operation fails
   */
  def main(args: Array[String]): Unit = {

    val mongoClient: MongoClient = if (args.isEmpty) MongoClient() else MongoClient(args.head)

    // get handle to "mydb" database
    val database: MongoDatabase = mongoClient.getDatabase("mydb")

    // get a handle to the "test" collection
    val collection: MongoCollection[Document] = database.getCollection("test")

    // Now an rxObservable!
    println("Dropping the test collection")
    val dropRx: rx.Observable[Completed] = collection.drop()
    assert(dropRx.toBlocking.first == Completed())

    // Insert some documents
    println("Inserting documents")
    val documents: IndexedSeq[Document] = (1 to 100) map { i: Int => Document("_id" -> i) }
    collection.insertMany(documents).toBlocking.first

    println("Finding documents")
    assert(collection.find().toList.toBlocking.head == documents)
  }
}
