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
 */
import java.io.PrintWriter
import java.util.logging.{Level, Logger}

import com.mongodb.ReadPreference
import com.mongodb.codecs.DocumentCodec
import org.bson.codecs.DecoderContext
import org.bson.json.JsonReader

import scala.Some

import org.mongodb.{Document, ReadPreference}
import org.mongodb.codecs.{DocumentCodec, PrimitiveCodecs}
import org.mongodb.json.JSONReader

import org.mongodb.scala.core.MongoClientURI
import org.mongodb.scala.rxscala.{MongoClient, MongoCollection}

import rx.lang.scala.Observable

/**
 * An example program providing similar functionality as the ``mongoexport`` program
 *
 * As there is no core CSV library for Scala CSV export is an exercise left to the reader.
 *
 * Add casbah-alldep jar to your path or add to ./lib directory and then run as a shell program:
 *
 *     ./mongoexport.scala -u mongodb://localhost/test.testData > ./data/testData.json
 *
 */
object mongoexport {
  val usage = """
                |Export MongoDB data to JSON files.
                |
                |Example:
                |  ./mongoexport.scala -u mongodb://localhost/test.testData > ./data/testData.json
                |
                |Options:
                |  --help                                produce help message
                |  --quiet                               silence all non error diagnostic
                |                                        messages
                |  -u [ --uri ] arg                      The connection URI - must contain a collection
                |                                        mongodb://[username:password@]host1[:port1][,host2[:port2]]/database.collection[?options]
                |                                        See: http://docs.mongodb.org/manual/reference/connection-string/
                |  -f [ --fields ] arg                   comma separated list of field names
                |                                        e.g. -f name,age
                |  -q [ --query ] arg                    query filter, as a JSON string, e.g.,
                |                                        '{x:{\$gt:1}}'
                |  -o [ --out ] arg                      output file; if not specified, stdout
                |                                        is used
                |  --jsonArray                           output to a json array rather than one
                |                                        object per line
                |  -k [ --slaveOk ] arg (=1)             use secondaries for export if
                |                                        available, default true
                |  --skip arg (=0)                       documents to skip, default 0
                |  --limit arg (=0)                      limit the numbers of documents
                |                                        returned, default all
                |  --sort arg                            sort order, as a JSON string, e.g.,
                |                                        '{x:1}'
              """.stripMargin

  /**
   * The main export program
   * Outputs debug information to Console.err - as Console.out is probably redirected to a file
   *
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

    // Get URI
    val mongoClientURI = MongoClientURI(options.uri.get)
    if (mongoClientURI.collection == None) {
      Console.err.println(s"Missing collection name in the URI eg:  mongodb://<hostInformation>/<database>.<collection>[?options]")
      Console.err.println(s"Current URI: $mongoClientURI")
      sys.exit(1)
    }

    // Turn off org.mongodb's noisy connection INFO logging
    // TODO - see if you can turn off logging
    Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.WARNING)
    Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.WARNING)

    // Get the collection
    val mongoClient = MongoClient(mongoClientURI)
    val collection: MongoCollection[Document] = mongoClient(mongoClientURI.database.get)(mongoClientURI.collection.get)

    // output
    val output: PrintWriter = options.out match {
      case None => new PrintWriter(System.out)
      case Some(fileName) => new PrintWriter(fileName)
    }

    if (!options.quiet) Console.err.print("Exporting...")

    // Export JSON
    val exported: Observable[Document] = exportJson(collection, output, options)
    if (!options.quiet) showPinWheel(exported)
    exported.toBlockingObservable.last

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
   * @param output the data source
   * @param options the configuration options
   */
  private def exportJson(collection: MongoCollection[Document], output: PrintWriter, options: Options): Observable[Document] = {

    val operation = collection.find(options.query)
    options.slaveOK match {
      case true => operation.withReadPreference(ReadPreference.secondaryPreferred())
      case false => operation
    }
    options.skip match {
      case None => operation
      case Some(value) => operation.skip(value)
    }
    options.limit match {
      case None => operation
      case Some(value) => operation.limit(value)
    }
    options.sort match {
      case None => operation
      case Some(value) => operation.sort(value)
    }

    val documents: Observable[Document] = operation.cursor()

    val (startString, endString, lineEndString) = options.jsonArray match {
      case true => ("[", "]\r\n", ",")
      case false => ("", "\r\n", "\r\n")
    }

    var firstLine = false
    documents.doOnEach(
      document => {
        firstLine match {
          case true => output.write(lineEndString)
          case false => output.write(startString)
        }
        output.write(document.toString)
        firstLine = true

    }, err => {
      Console.err.println(s"Error: ${err.getMessage}")
      output.close()
    },
    () => {
      output.write(endString)
      output.close()
    })
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
      case "-f" :: value :: tail =>
        parseArgs(map ++ Map("fields" -> value.split(",").toList), tail)
      case "--file" :: value :: tail =>
        parseArgs(map ++ Map("fields" -> value.split(",").toList), tail)
      case "-q" :: value :: tail =>
        parseArgs(map ++ Map("query" -> value), tail)
      case "--query" :: value :: tail =>
        parseArgs(map ++ Map("query" -> value), tail)
      case "-o" :: value :: tail =>
        parseArgs(map ++ Map("out" -> value), tail)
      case "--out" :: value :: tail =>
        parseArgs(map ++ Map("out" -> value), tail)
      case "-k" :: value :: tail =>
        parseArgs(map ++ Map("slaveOk" -> value), tail)
      case "--slaveOk" :: value :: tail =>
        parseArgs(map ++ Map("slaveOk" -> value), tail)
      case "--skip" :: value :: tail =>
        parseArgs(map ++ Map("skip" -> value), tail)
      case "--limit" :: value :: tail =>
        parseArgs(map ++ Map("limit" -> value), tail)
      case "--sort" :: value :: tail =>
        parseArgs(map ++ Map("sort" -> value), tail)
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
    val options = Options(
      quiet = optionMap.getOrElse("quiet", default.quiet).asInstanceOf[Boolean],
      uri = optionMap.get("uri") match {
        case None => default.uri
        case Some(value) => Some(value.asInstanceOf[String])
      },
      out = optionMap.get("out") match {
        case None => default.out
        case Some(value) => Some(value.asInstanceOf[String])
      },
      slaveOK = optionMap.getOrElse("slaveOK", default.slaveOK).asInstanceOf[Boolean],
      fields = optionMap.get("fields") match {
        case None => default.fields
        case Some(value) => Some(value.asInstanceOf[List[String]])
      },
      query = optionMap.get("query") match {
        case None => default.query
        case Some(value) => documentFromString(value.asInstanceOf[String])
      },
      sort = optionMap.get("sort") match {
        case None => default.sort
        case Some(value) => Some(documentFromString(value.asInstanceOf[String]))
      },
      skip = optionMap.get("skip") match {
        case None => default.skip
        case Some(value) => Some(value.asInstanceOf[Int])
      },
      limit = optionMap.get("limit") match {
        case None => default.limit
        case Some(value) => Some(value.asInstanceOf[Int])
      },
      jsonArray = optionMap.getOrElse("jsonArray", default.jsonArray).asInstanceOf[Boolean]
    )
    if (options.out == None && !options.quiet) options.copy(quiet=true)
    else options
  }

  /**
   * Convert a json string to a document
   * @param string the json string
   * @return a document
   */
  private def documentFromString(string: String): Document = {
    val jsonReader: JsonReader = new JsonReader(string)
    new DocumentCodec().decode(jsonReader, DecoderContext.builder().build())
  }

  case class Options(quiet: Boolean = false, uri: Option[String] = None, out: Option[String] = None,
                     slaveOK: Boolean = true, fields: Option[List[String]] = None, query: Document = new Document(),
                     sort: Option[Document] = None, skip: Option[Int] = None, limit: Option[Int] = None,
                     jsonArray: Boolean = false)

  private def currentTime = System.currentTimeMillis()

  /**
   * Shows a pinWheel in the console.err
   * @param observable the observable we are all waiting for
   */
  private def showPinWheel(observable: Observable[_]) {
    // Let the user know something is happening until observable isCompleted
    var spin = true
    observable.doOnCompleted(() => spin=false)
    observable.doOnError(e => spin=false)

    val spinChars = List("|", "/", "-", "\\")
    while (spin) {
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

mongoexport.main(args)
