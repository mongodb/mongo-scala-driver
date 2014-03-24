#!/bin/sh
L=`pwd`
cp=`echo $L/lib/*`
exec scala -cp "$cp" "$0" "$@"
!#

/**
 * Copyright (c) 2014 MongoDB, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * For questions and comments about this product, please see the project page at:
 *
 * https://github.com/mongodb/mongo-scala-driver
 *
 */



import scala.Some
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.{BufferedSource, Source}

import org.mongodb.{Document, WriteResult}
import org.mongodb.codecs._
import org.mongodb.json.JSONReader
import org.mongodb.scala._

import java.util.logging.{Level, Logger}
/**
 * An example program providing similar functionality as the ``mongoimport`` program
 *
 * As there is no core CSV library for Scala CSV import is an exercise left to the reader
 *
 * Add mongo-scala-driver-alldep jar to your path or add to ./lib directory and then run as a shell program::
 *
 *    ./mongoimport.scala -u mongodb://localhost/test.testData --drop < data/testData.json
 *
 */
object mongoimport {
  val usage = """
                |Import JSON data into MongoDB using Mongo-Scala-Driver
                |
                |When importing JSON documents, each document must be a separate line of the input file.
                |
                |Example:
                |  mongoimport --uri mongodb://localhost/my_db.my_collection < mydocfile.json
                |
                |Options:
                |  --help                                produce help message
                |  --quiet                               silence all non error diagnostic
                |                                        messages
                |  -u [ --uri ] arg                      The connection URI - must contain a collection
                |                                        mongodb://[username:password@]host1[:port1][,host2[:port2]]/database.collection[?options]
                |                                        See: http://docs.mongodb.org/manual/reference/connection-string/
                |  --file arg                            file to import from; if not specified
                |                                        stdin is used
                |  --drop                                drop collection first
                |  --jsonArray                           load a json array, not one item per
                |                                        line. Currently limited to 16MB.
              """.stripMargin


  /**
   * The main export program
   * @param args the commandline arguments
   */
  def main(args: Array[String]) {

    // The time when the execution of this program started, in milliseconds since 1 January 1970 UTC.
    val executionStart: Long = currentTime

    // Output help?
    if (args.length == 0 | args.contains("--help")) {
      Console.err.println(usage)
      sys.exit(1)
    }

    // Parse args
    val optionMap = parseArgs(Map(), args.toList)
    val options = getOptions(optionMap)

    // Check for uri
    if (options.uri == None) {
      Console.err.println(s"Missing URI")
      Console.err.println(usage)
      sys.exit(1)
    }

    // Get source
    val importSource: BufferedSource = options.file match {
      case None => Source.stdin
      case Some(fileName) => Source.fromFile(fileName)
    }

    // Get URI
    val mongoClientURI = MongoClientURI(options.uri.get)
    if (mongoClientURI.collection == None) {
      Console.err.println(s"Missing collection name in the URI eg:  mongodb://<hostInformation>/<database>.<collection>[?options]")
      Console.err.println(s"Current URI: $mongoClientURI")
      sys.exit(1)
    }

    // Turn off org.mongodb's noisy connection INFO logging - only works with the JULLogger
    Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.WARNING)
    Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.WARNING)

    // Get the collection
    val mongoClient = MongoClient(mongoClientURI)
    val collection: MongoCollection[Document] = mongoClient(mongoClientURI.database.get)(mongoClientURI.collection.get)

    // Drop the database?
    if (options.drop) {
      val name = collection.name
      if (!options.quiet) Console.err.print(s"Dropping: $name ")
      val dropFuture = collection.admin.drop()
      if (!options.quiet) showPinWheel(dropFuture)
      Await.result(dropFuture, Duration.Inf)
    }

    // Import JSON in a future so we can output a spinner
    if (!options.quiet) Console.err.print("Importing...")

    // Import the JSON
    val importPromise = Promise[ListBuffer[WriteResult]]()
    val promisedFuture = importPromise.future
    Future(importPromise completeWith Future.sequence(importJson(collection, importSource, options)))
    if (!options.quiet) showPinWheel(promisedFuture)
    Await.result(promisedFuture, Duration.Inf)

    // Close the client
    collection.client.close()

    // Output execution time
    val total = currentTime - executionStart
    if (!options.quiet) Console.err.println(s"Finished: $total ms")
  }

  /**
   * Imports JSON into the collection
   *
   * @param collection the collection to import into
   * @param importSource the data source
   * @param options the configuration options
   */
  private def importJson(collection: MongoCollection[Document], importSource: BufferedSource, options: Options): ListBuffer[Future[WriteResult]] = {
    options.jsonArray match {
      case true =>
        // Import all
        val jsonReader: JSONReader = new JSONReader(importSource.mkString)
        val iterableCodec = new IterableCodec(Codecs.createDefault())
        val documents: Iterable[Document] = iterableCodec.decode[Document](jsonReader).asScala
        ListBuffer(collection.insert(documents))
      case false =>
        // Import in batches of 1000
        val documentCodec = new DocumentCodec(PrimitiveCodecs.createDefault)
        val lines = importSource.getLines()
        val futures = ListBuffer[Future[WriteResult]]()
        while (lines.hasNext) {
          val batch = lines.take(1000)
          val documents: Iterable[Document] = batch.map {case line =>
            val jsonReader: JSONReader = new JSONReader(line)
            documentCodec.decode(jsonReader)
          }.toIterable
          futures += collection.insert(documents)
        }
        futures
    }
  }

  /**
   * Recursively convert the args list into a Map of options
   *
   * @param map - the initial option map
   * @param args - the args list
   * @return the parsed OptionMap
   */
  private def parseArgs(map: Map[String, Any], args: List[String]): Map[String, Any] = {
    args match {
      case Nil => map
      case "--quiet" :: tail =>
        parseArgs(map ++ Map("quiet" -> true), tail)
      case "-u" :: value :: tail =>
        parseArgs(map ++ Map("uri" -> value), tail)
      case "--uri" :: value :: tail =>
        parseArgs(map ++ Map("uri" -> value), tail)
      case "--file" :: value :: tail =>
        parseArgs(map ++ Map("file" -> value), tail)
      case "--drop" :: tail =>
        parseArgs(map ++ Map("drop" -> true), tail)
      case "--jsonArray" :: tail =>
        parseArgs(map ++ Map("jsonArray" -> true), tail)
      case option :: tail =>
        Console.err.println("Unknown option " + option)
        Console.err.println(usage)
        sys.exit(1)
    }
  }

  /**
   * Convert the optionMap to an Options instance
   * @param optionMap the parsed args options
   * @return Options instance
   */
  private def getOptions(optionMap: Map[String, _]): Options = {
    val default = Options()
    Options(
      quiet = optionMap.getOrElse("quiet", default.quiet).asInstanceOf[Boolean],
      uri = optionMap.get("uri") match {
        case None => default.uri
        case Some(value) => Some(value.asInstanceOf[String])
      },
      file = optionMap.get("file") match {
        case None => default.file
        case Some(value) => Some(value.asInstanceOf[String])
      },
      drop = optionMap.getOrElse("drop", default.drop).asInstanceOf[Boolean],
      jsonArray = optionMap.getOrElse("jsonArray", default.jsonArray).asInstanceOf[Boolean]
    )
  }

  case class Options(quiet: Boolean = false, uri: Option[String] = None, file: Option[String] = None,
                     drop: Boolean = false, jsonArray: Boolean = false)

  private def currentTime = System.currentTimeMillis()

  /**
   * Shows a pinWheel in the console.err
   * @param someFuture the future we are all waiting for
   */
  private def showPinWheel(someFuture: Future[_]) {
    // Let the user know something is happening until futureOutput isCompleted
    val spinChars = List("|", "/", "-", "\\")
    while (!someFuture.isCompleted) {
      spinChars.foreach({
        case char =>
          Console.err.print(char)
          Thread sleep 200
          Console.err.print("\b")
      })
    }
    Console.err.print(" ")
    Console.err.println("")
  }

}

mongoimport.main(args)
