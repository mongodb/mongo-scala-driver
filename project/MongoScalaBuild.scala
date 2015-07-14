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

import scala.util.matching.Regex.Match

import com.typesafe.sbt.SbtScalariform._
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
    organization := "org.mongodb.scala",
    organizationHomepage := Some(url("http://www.mongodb.org")),
    version := "1.0.0-SNAPSHOT",
    scalaVersion := scalaCoreVersion,
    libraryDependencies ++= coreDependencies,
    resolvers := mongoScalaResolvers,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint", "-Xlint:-missing-interpolator" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer"*/)
  )

  val publishSettings = Publish.settings
  val noPublishSettings = Publish.noPublishing

  /*
   * Test Settings
   */
  val testSettings = Seq(
    testFrameworks += TestFrameworks.ScalaTest,
    ScoverageKeys.coverageMinimum := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    libraryDependencies ++= testDependencies
  ) ++ Defaults.itSettings

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

  // Check style
  val checkAlias = addCommandAlias("check", ";clean;scalastyle;coverage;test;it:test;coverageAggregate;coverageReport")

  // Documentation Settings to link to the async JavaDoc
  val apiUrl = "http://api.mongodb.org/java/3.1/"
  val docSettings = Seq(
    autoAPIMappings := true,
    apiMappings ++= {
      def findManagedDependency(organization: String, name: String): Option[File] = {
        (for {
          entry <- (fullClasspath in Runtime).value ++ (fullClasspath in Test).value
          module <- entry.get(moduleID.key) if module.organization == organization && module.name.startsWith(name)
        } yield entry.data).headOption
      }
      val links = Seq(
        findManagedDependency("org.mongodb", "mongodb-driver-async").map(d => d -> url(apiUrl))
      )
      links.collect { case Some(d) => d }.toMap
    }
  ) ++ fixJavaLinksSettings

  lazy val fixJavaLinks = taskKey[Unit]("Fix Java links in scaladoc - replace #java.io.File with ?java/io/File.html" )
  lazy val fixJavaLinksSettings = fixJavaLinks := {
    val t = (target in UnidocKeys.unidoc).value
    (t ** "*.html").get.filter(hasJavadocApiLink).foreach { f =>
      val newContent = javadocApiLink.replaceAllIn(IO.read(f), fixJavaLinksMatch)
      IO.write(f, newContent)
    }
  }
  val fixJavaLinksMatch: Match => String = m => m.group(1) + "?" + m.group(2).replace(".", "/") + ".html"
  val javadocApiLink = List("\"(\\Q", apiUrl, "index.html\\E)#([^\"]*)\"").mkString.r
  def hasJavadocApiLink(f: File): Boolean = (javadocApiLink findFirstIn IO.read(f)).nonEmpty

  val rootUnidocSettings = Seq(
    scalacOptions in(Compile, doc) ++= Opts.doc.title("Mongo Scala Driver"),
    scalacOptions in(Compile, doc) ++= Seq("-diagrams", "-unchecked", "-doc-root-content", "rootdoc.txt")
  ) ++ docSettings ++ unidocSettings ++ Seq(fixJavaLinks <<= fixJavaLinks triggeredBy (doc in ScalaUnidoc))

  lazy val driver = Project(
    id = "mongo-scala-driver",
    base = file("driver")
  ).configs(IntegrationTest)
    .configs(UnitTest)
    .settings(buildSettings)
    .settings(testSettings)
    .settings(customScalariformSettings)
    .settings(scalaStyleSettings)
    .settings(scoverageSettings)
    .settings(docSettings)
    .settings(publishSettings)
    .dependsOn(bson)

  lazy val bson = Project(
    id = "mongo-scala-bson",
    base = file("bson")
  ).configs(IntegrationTest)
    .configs(UnitTest)
    .settings(buildSettings)
    .settings(testSettings)
    .settings(scalaStyleSettings)
    .settings(docSettings)
    .settings(publishSettings)

  lazy val examples = Project(
    id = "mongo-scala-driver-examples",
    base = file("examples")
  ).aggregate(bson)
    .aggregate(driver)
    .settings(buildSettings)
    .settings(scalaStyleSettings)
    .settings(noPublishSettings)
    .dependsOn(driver)

  lazy val root = Project(
    id = "mongo-scala",
    base = file(".")
  ).aggregate(bson)
    .aggregate(driver)
    .settings(buildSettings)
    .settings(scalaStyleSettings)
    .settings(scoverageSettings)
    .settings(rootUnidocSettings)
    .settings(noPublishSettings)
    .settings(checkAlias)
    .settings(initialCommands in console := """import org.mongodb.scala._""")
    .dependsOn(driver)

  override def rootProject = Some(root)

}
