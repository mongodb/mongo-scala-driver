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

import scala.language.implicitConversions

import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks

import ch.qos.logback.classic.{Logger, LoggerContext}
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import org.slf4j.LoggerFactory

trait UnitTestSpec extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  var logCapturingStream: ByteArrayOutputStream = null

  def withLogCapture(name: String)(testCode: Logger => Any) {

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    val encoder: PatternLayoutEncoder = new PatternLayoutEncoder()
    encoder.setContext(context)
    encoder.setPattern("%-5level: %msg%n")
    encoder.start()

    logCapturingStream = new ByteArrayOutputStream()

    val appender: OutputStreamAppender[ILoggingEvent] = new OutputStreamAppender[ILoggingEvent]()
    appender.setName("OutputStream Appender")
    appender.setContext(context)
    appender.setEncoder(encoder)
    appender.setOutputStream(logCapturingStream)
    appender.start()

    val logger: Logger = context.getLogger(s"org.mongodb.driver.$name")
    logger.addAppender(appender)

    try testCode(logger) // "loan" the fixture to the test
    finally {
      logCapturingStream.close()
      appender.stop()
      logger.detachAppender(appender)
    }
  }

  def getLogMessages: Set[String] = {
    logCapturingStream.toString.split("\n").toSet
  }

}

