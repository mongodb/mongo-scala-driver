/*
  * Copyright 2015 MongoDB, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import sbt._

object Dependencies {
  // Versions
  val scalaCoreVersion     = "2.11.5"
  val mongodbDriverVersion = "1.0.0-SNAPSHOT"

  val scalaTestVersion     = "2.2.1"
  val scalaMockVersion     = "3.2.1"
  val logbackVersion       = "1.1.2"

  // Scala
  val scalaReflection    = "org.scala-lang" % "scala-reflect" % scalaCoreVersion
  val scalaCompiler      = "org.scala-lang" % "scala-compiler" % scalaCoreVersion

  // Libraries
  val mongodbDriver = "org.mongodb" % "mongodb-driver-reactivestreams" % mongodbDriverVersion

  // Test
  val scalaTest     = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  val scalaMock     = "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % "test"
  val logback       = "ch.qos.logback" % "logback-classic"  % logbackVersion % "test"

  // Projects
  val coreDependencies = Seq(scalaCompiler, scalaReflection, mongodbDriver)
  val testDependencies = Seq(scalaTest, scalaMock, logback)
}
