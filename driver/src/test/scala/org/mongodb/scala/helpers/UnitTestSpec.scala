/**
 * Copyright 2010-2014 MongoDB, Inc. <http://www.mongodb.org>
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
package org.mongodb.scala.helpers

import java.io.ByteArrayOutputStream
import java.util.logging._

import scala.language.implicitConversions

import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks


trait UnitTestSpec extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  var logCapturingStream: ByteArrayOutputStream = null
  var customLogHandler: StreamHandler = null


  def withLogCapture(name: String)(testCode: StreamHandler => Any) {
    val logger: Logger = Logger.getLogger(s"org.mongodb.driver.$name")
    logCapturingStream = new ByteArrayOutputStream()
    val handlers = logger.getParent.getHandlers
    customLogHandler = new StreamHandler(logCapturingStream, new SimpleLineFormatter())
    logger.addHandler(customLogHandler)
    logger.setUseParentHandlers(false)

    try testCode(customLogHandler) // "loan" the fixture to the test
    finally {
      for (handler <- handlers) logger.addHandler(handler)
      logger.setUseParentHandlers(true)
      customLogHandler.close()
      logCapturingStream.close()
    }
  }

  def getLogMessages: Set[String] = {
    customLogHandler.flush()
    logCapturingStream.toString.split("\n").toSet
  }

}

class SimpleLineFormatter extends SimpleFormatter {

  override def format(record: LogRecord) = {
    val level = record.getLevel.getLocalizedName
    val message = record.getMessage
    s"$level: $message\n"
  }

}
