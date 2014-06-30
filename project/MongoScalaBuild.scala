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

import com.typesafe.sbt._
import SbtSite._
import SiteKeys._
import SbtGit._
import GitKeys._
import SbtGhPages._
import GhPagesKeys._
import org.scalastyle.sbt.ScalastylePlugin
import sbtunidoc.Plugin._
import sbtassembly.Plugin._
import sbt._
import Keys._
import AssemblyKeys._
import SbtScalariform._

object MongoScalaBuild extends Build {

  import Dependencies._
  import Resolvers._

  val buildSettings = Seq(
    organization := "org.mongodb.scala",
    organizationHomepage := Some(url("http://www.mongodb.org")),
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.0",
    libraryDependencies ++= coreDependencies ++ testDependencies,
    resolvers := mongoScalaResolvers,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer" */),
    scalacOptions in(Compile, doc) ++= Seq("-diagrams")
  )

  /*
   * Documentation
   */
  val docSettings =
    SbtSite.site.settings ++
      SbtSite.site.sphinxSupport() ++
      ghpages.settings ++
      unidocSettings ++
      Seq(
        siteSourceDirectory := file("docs"),
        siteDirectory := file("target/site"),
        // depending on the version, copy the api files to a different directory
        siteMappings <++= (mappings in packageDoc in ScalaUnidoc, version) map {
          (m, v) =>
            for ((f, d) <- m) yield (f, if (v.trim.endsWith("SNAPSHOT")) ("api/master/" + d) else ("api/" + v + "/" + d))
        },
        // override the synchLocal task to avoid removing the existing files
        synchLocal <<= (privateMappings, updatedRepository, ghpagesNoJekyll, gitRunner, streams) map {
          (mappings, repo, noJekyll, git, s) =>
            val betterMappings = mappings map {
              case (file, target) => (file, repo / target)
            }
            IO.copy(betterMappings)
            if (noJekyll) IO.touch(repo / ".nojekyll")
            repo
        },
        ghpagesNoJekyll := true,
        gitRemoteRepo := "git@github.com:mongodb/mongo-scala-driver.git"
      ) ++ inConfig(config("sphinx"))(Seq(sourceDirectory := file("docs")))

  val publishSettings = Publish.settings


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
    ScalariformKeys.preferences in Test    := scalariFormFormattingPreferences
  )
  val scalaStyleSettings = ScalastylePlugin.Settings ++ Seq(org.scalastyle.sbt.PluginKeys.config := file("project/scalastyle-config.xml"))

  /*
   * Assembly Jar Settings
   */
  val asyncAssemblyJarSettings = assemblySettings ++ addArtifact(Artifact("mongo-scala-async-alldep", "jar", "jar"), assembly) ++ Seq(test in assembly := {})
  val rxScalaAssemblyJarSettings = assemblySettings ++ addArtifact(Artifact("mongo-scala-rxscala-alldep", "jar", "jar"), assembly) ++ Seq(test in assembly := {})

  // Test configuration
  val testSettings = Seq(
    testFrameworks += TestFrameworks.ScalaTest,
    testFrameworks in PerfTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),
    testOptions in Test := Seq(Tests.Filter(testFilter)),
    testOptions in AccTest := Seq(Tests.Filter(accFilter)),
    testOptions in IntTest := Seq(Tests.Filter(itFilter)),
    testOptions in UnitTest := Seq(Tests.Filter(unitFilter)),
    testOptions in PerfTest := Seq(Tests.Filter(perfFilter)),
    parallelExecution in PerfTest := false,
    logBuffered in PerfTest := false
  ) ++ Seq(AccTest, IntTest, UnitTest, PerfTest).flatMap {
    inConfig(_)(Defaults.testTasks)
  }

  def accFilter(name: String): Boolean = name endsWith "ASpec"

  def itFilter(name: String): Boolean = name endsWith "ISpec"

  def perfFilter(name: String): Boolean = name endsWith "Benchmark"

  def unitFilter(name: String): Boolean = !itFilter(name) && !accFilter(name) && !perfFilter(name)

  def testFilter(name: String): Boolean = !perfFilter(name)

  lazy val IntTest = config("it") extend Test
  lazy val UnitTest = config("unit") extend Test
  lazy val AccTest = config("acc") extend Test
  lazy val PerfTest = config("perf") extend Test

  /*
   * Coursera styleCheck command
   */
  val styleCheck = TaskKey[Unit]("styleCheck")

  /**
   * depend on compile to make sure the sources pass the compiler
   */
  val styleCheckSetting = styleCheck <<= (compile in Compile, sources in Compile, streams) map {
    (_, sourceFiles, s) =>
      val logger = s.log
      val (feedback, score) = StyleChecker.assess(sourceFiles)
      logger.info(feedback)
      logger.info(s"Style Score: $score out of ${StyleChecker.maxResult}")
  }

  lazy val core = Project(
    id = "core",
    base = file("driver-core")
  ).configs(UnitTest)
    .settings(buildSettings: _*)
    .settings(styleCheckSetting: _*)
    .settings(scalaStyleSettings: _*)
    .settings(customScalariformSettings: _*)

  lazy val async = Project(
    id = "async",
    base = file("driver-async")
  ).configs(IntTest)
    .configs(AccTest)
    .configs(UnitTest)
    .configs(PerfTest)
    .settings(buildSettings: _*)
    .settings(testSettings: _*)
    .settings(styleCheckSetting: _*)
    .settings(scalaStyleSettings: _*)
    .settings(publishSettings: _*)
    .settings(asyncAssemblyJarSettings: _*)
    .settings(initialCommands in console := """import org.mongodb.scala._""")
    .dependsOn(core)

  lazy val rxscala = Project(
    id = "rxscala",
    base = file("driver-rxscala")
  ).configs(IntTest)
    .configs(AccTest)
    .configs(UnitTest)
    .configs(PerfTest)
    .settings(buildSettings: _*)
    .settings(testSettings: _*)
    .settings(styleCheckSetting: _*)
    .settings(scalaStyleSettings: _*)
    .settings(libraryDependencies ++= rxScalaDependencies)
    .settings(rxScalaAssemblyJarSettings: _*)
    .dependsOn(core)

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).aggregate(core, async, rxscala)
    .dependsOn(core, async, rxscala)
    .settings(buildSettings: _*)
    .settings(docSettings: _*)

  override def rootProject = Some(root)

}
