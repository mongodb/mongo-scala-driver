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

import java.io.FileInputStream
import java.util.Properties

import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._

object Publish {

  val propFile = new File(Path.userHome / ".gradle", "gradle.properties")

  val username = "nexusUsername"
  val password = "nexusPassword"
  val keyId = "signing.keyId"
  val secretKeyRing = "signing.secretKeyRingFile"
  val keyPassword = "signing.password"

  lazy val settings: Seq[Def.Setting[_]] = {
    if (!propFile.exists) Seq.empty
    else {
      val props = new Properties
      val input = new FileInputStream(propFile)
      try props.load(input) finally input.close()

      mavenSettings ++ SbtPgp.settings ++ Seq(
        SbtPgp.pgpPassphrase := Some(props.getProperty(keyPassword).toArray),
        SbtPgp.pgpSecretRing := file(props.getProperty(secretKeyRing)),
        credentials += Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          props.getProperty(username),
          props.getProperty(password)),
        publishSnapshot <<= publishSnapshotTask
      ) ++ Seq(
        assemblyJarName in assembly := "mongo-scala-driver-alldep.jar",
        test in assembly := {},
        artifact in (Compile, assembly) := {
        val art = (artifact in (Compile, assembly)).value
        art.copy(`classifier` = Some("alldep"))
        }
      ) ++ addArtifact(artifact in (Compile, assembly), assembly)
    }
  }

  lazy val mavenSettings = Seq(
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>http://mongodb.github.io/mongo-scala-driver</url>
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
      )
  )

  lazy val noPublishing = Seq(
    publish :=(),
    publishLocal :=(),
    publishTo := None,
    publishSnapshot := None
  )

  lazy val publishSnapshot: TaskKey[Unit] = TaskKey[Unit]("publish-snapshot", "publishes a snapshot")
  val publishSnapshotTask = Def.taskDyn {
    // Only publish if snapshot
    if(isSnapshot.value) Def.task { PgpKeys.publishSigned.value } else Def.task { }
  }

}
