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
import sbt.Keys._
import sbt.Def.Initialize
import scala.xml.NodeBuffer

object Publish {

  lazy val settings = Seq(
    crossPaths := false,
    pomExtra := driverPomExtra,
    publishTo <<= sonatypePublishTo,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    pomIncludeRepository := { x => false },
    publishMavenStyle := true,
    publishArtifact in Test := false
  )

  def sonatypePublishTo: Initialize[Option[Resolver]] = {
    version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  }

  def driverPomExtra: NodeBuffer = {
    <url>http://github.com/mongodb/mongo-scala-driver</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:mongodb/mongo-scala-driver.git</url>
      <connection>scm:git:git@github.com:mongodb/mongo-scala-driver.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ross</id>
        <name>Ross Lawley</name>
        <url>http://rosslawley.co.uk</url>
      </developer>
    </developers>
  }

}
