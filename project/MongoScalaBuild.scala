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

import com.typesafe.sbt.SbtScalariform._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import scoverage.ScoverageSbtPlugin._

object MongoScalaBuild extends Build {

  import Dependencies._
  import Resolvers._

  val buildSettings = Seq(
    organization := "org.mongodb.scala",
    organizationHomepage := Some(url("http://www.mongodb.org")),
    version := "1.0.0-SNAPSHOT",
    scalaVersion := scalaCoreVersion,
    libraryDependencies ++= coreDependencies ++ testDependencies,
    resolvers := mongoScalaResolvers,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer"*/),
    scalacOptions in(Compile, doc) ++= Seq("-diagrams", "-unchecked", "-doc-root-content", "driver/rootdoc.txt")
  )

  val publishSettings = Publish.settings

  /*
   * Test Settings
   */
  val testSettings = Seq(
    testFrameworks += TestFrameworks.ScalaTest,
    testOptions in IntTest := Seq(Tests.Filter(itFilter)),
    testOptions in UnitTest <<= testOptions in Test,
    testOptions in UnitTest += Tests.Filter(unitFilter),
    ScoverageKeys.coverageMinimum := 95,
    ScoverageKeys.coverageFailOnMinimum := true
  ) ++ Seq(IntTest, UnitTest).flatMap {
    inConfig(_)(Defaults.testTasks)
  }

  def itFilter(name: String): Boolean = name endsWith "ISpec"
  def unitFilter(name: String): Boolean = !itFilter(name)

  lazy val IntTest = config("it") extend Test
  lazy val UnitTest = config("unit") extend Test

  val scoverageSettings = Seq()

  /*
   * Style and formatting
   */
  def scalariFormFormattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  }

  val customScalariformSettings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := scalariFormFormattingPreferences,
    ScalariformKeys.preferences in Test := scalariFormFormattingPreferences
  )

  val scalaStyleSettings = Seq(
    (scalastyleConfig in Compile) := file("project/scalastyle-config.xml"),
    (scalastyleConfig in Test) := file("project/scalastyle-config.xml")
  )

  /*
   * Assembly Jar Settings
   */
  val driverAssemblyJarSettings = assemblySettings ++
    addArtifact(Artifact("mongo-scala-driver-alldep", "jar", "jar"), assembly) ++ Seq(test in assembly := {})

  // Check style task
  val checkTask = TaskKey[Unit]("check", "Runs scalastyle, test and coverage") := {
    (scalastyle in Compile).toTask("").value
    (test in Test).value
    (ScoverageKeys.coverage in Test).value
    (ScoverageKeys.coverageReport in Test).value
    (ScoverageKeys.coverageAggregate in Test).value
  }


  lazy val driver = Project(
    id = "driver",
    base = file("driver")
  ).configs(IntTest)
    .configs(UnitTest)
    .settings(buildSettings: _*)
    .settings(testSettings: _*)
    .settings(publishSettings: _*)
    .settings(driverAssemblyJarSettings: _*)
    .settings(customScalariformSettings: _*)
    .settings(scalaStyleSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(initialCommands in console := """import com.mongodb.scala._""")
    .settings(checkTask)
    .dependsOn(core)

  lazy val core = Project(
    id = "core",
    base = file("core")
  ).configs(IntTest)
    .configs(UnitTest)
    .settings(buildSettings: _*)
    .settings(testSettings: _*)
    .settings(scalaStyleSettings: _*)
    .settings(scalaVersion := scalaCoreVersion)
    .settings(checkTask)

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).aggregate(core)
    .aggregate(driver)
    .settings(buildSettings: _*)
    .settings(scalaStyleSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(publish := {}, publishLocal := {})

  override def rootProject = Some(root)

}
