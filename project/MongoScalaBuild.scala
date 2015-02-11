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

import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtScalariform._
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import sbtunidoc.Plugin._
import scoverage.ScoverageSbtPlugin._

object MongoScalaBuild extends Build {

  import Dependencies._
  import Resolvers._

  val buildSettings = Seq(
    organization := "com.mongodb.scala",
    organizationHomepage := Some(url("http://www.mongodb.org")),
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    libraryDependencies ++= coreDependencies ++ testDependencies,
    resolvers := mongoScalaResolvers,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer"*/),
    scalacOptions in(Compile, doc) ++= Seq("-diagrams", "-implicits", "-unchecked", "-doc-root-content", "driver/rootdoc.txt")
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
   * Test Settings
   */
  val testSettings = Seq(
    testFrameworks += TestFrameworks.ScalaTest,
    testOptions in IntTest := Seq(Tests.Filter(itFilter)),
    testOptions in UnitTest <<= testOptions in Test,
    testOptions in UnitTest += Tests.Filter(unitFilter),
    ScoverageKeys.coverageMinimum := 100,
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
    ScalariformKeys.preferences in Test    := scalariFormFormattingPreferences
  )

  val scalaStyleSettings = Seq(
    (scalastyleConfig in Compile) := file("project/scalastyle-config.xml")
  )

  /*
   * Assembly Jar Settings
   */
  val reactiveStreamsAssemblyJarSettings = assemblySettings ++
    addArtifact(Artifact("mongo-scala-reactivestreams-alldep", "jar", "jar"), assembly) ++ Seq(test in assembly := {})

  lazy val reactiveStreams = Project(
    id = "reactiveStreams",
    base = file("driver")
  ).configs(IntTest)
    .configs(UnitTest)
    .settings(buildSettings: _*)
    .settings(testSettings: _*)
    .settings(publishSettings: _*)
    .settings(reactiveStreamsAssemblyJarSettings: _*)
    .settings(customScalariformSettings: _*)
    .settings(scalaStyleSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(initialCommands in console := """import com.mongodb.scala._""")

  lazy val root = Project(
    id = "root",
    base = file(".")
  ).aggregate(reactiveStreams)
    .dependsOn(reactiveStreams)
    .settings(buildSettings: _*)
    .settings(docSettings: _*)
    .settings(scalaStyleSettings: _*)
    .settings(scoverageSettings: _*)

  override def rootProject = Some(root)

}
