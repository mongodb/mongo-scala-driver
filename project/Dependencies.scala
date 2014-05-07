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
import sbt._

object Dependencies {
  // Versions
  val scalaCoreVersion     = "2.11.0"

  val mongodbDriverVersion = "3.0.0-SNAPSHOT"
  val rxJavaScalaVersion   = "0.18.2"

  val scalaTestVersion     = "2.1.5"
  val scalaMeterVersion    = "0.5-SNAPSHOT"
  val logbackVersion       = "1.1.1"


  // Scala
  val scalaReflection    = "org.scala-lang" % "scala-reflect" % scalaCoreVersion
  val scalaCompiler      = "org.scala-lang" % "scala-compiler" % scalaCoreVersion

  // Libraries
  val mongodbDriver = "org.mongodb" % "mongodb-driver" % mongodbDriverVersion
  val rxJavaScala   = "com.netflix.rxjava" % "rxjava-scala" % rxJavaScalaVersion

  // Test
  val scalaTest     = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val scalaMeter    = "com.github.axel22" %% "scalameter" % scalaMeterVersion % "test"
  val logback       = "ch.qos.logback" % "logback-classic"  % logbackVersion % "test"

  // Projects
  val coreDependencies = Seq(scalaCompiler, scalaReflection, mongodbDriver, rxJavaScala)
  val testDependencies = Seq(scalaTest, scalaMeter, logback)
}
