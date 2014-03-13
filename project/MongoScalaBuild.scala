import com.typesafe.sbt.SbtSite.{site, SiteKeys}
import org.scalastyle.sbt.ScalastylePlugin
import sbtassembly.Plugin._
import sbt._
import Keys._
import scala.Some
import AssemblyKeys._


object MongoScalaBuild extends Build {

  import Dependencies._
  import Resolvers._

  val buildSettings = Seq(
    organization := "org.mongodb",
    organizationHomepage := Some(url("http://www.mongodb.org")),
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.0-RC1",
    libraryDependencies ++= coreDependencies ++ testDependencies,
    resolvers := mongoScalaResolvers,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer" */),
    scalacOptions in(Compile, doc) ++= Seq("-diagrams", "-implicits")
  )

  val consoleSettings = Seq(initialCommands in console := """import org.mongodb.scala._""")

  val docSettings = site.settings ++ site.includeScaladoc() ++ site.sphinxSupport("latest") ++ Seq(
    SiteKeys.siteSourceDirectory := file("docs"),
    SiteKeys.siteDirectory := file("target/site")
  ) ++ inConfig(config("sphinx"))(Seq(sourceDirectory := file("docs")))

  val scalaStyleSettings = ScalastylePlugin.Settings ++ Seq(org.scalastyle.sbt.PluginKeys.config := file("project/scalastyle-config.xml"))
  val publishSettings = Publish.settings
  val assemblyJarSettings = assemblySettings ++ addArtifact(Artifact("mongo-scala-driver-alldep", "jar", "jar"), assembly) ++ Seq(test in assembly := {})

  // Test configuration
  val testSettings = Seq(
    testFrameworks += TestFrameworks.ScalaTest,
    testFrameworks in PerfTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),
    testOptions in Test := Seq(Tests.Filter(testFilter)),
    testOptions in AccTest := Seq(Tests.Filter(accFilter)),
    testOptions in IntTest := Seq(Tests.Filter(itFilter)),
    testOptions in UnitTest := Seq(Tests.Filter(unitFilter)),
    testOptions in PerfTest := Seq(Tests.Filter(perfFilter)),
    logBuffered in PerfTest := false
  ) ++ Seq(AccTest, IntTest, UnitTest, PerfTest).flatMap { inConfig(_)(Defaults.testTasks) }

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
  val styleCheckSetting = styleCheck <<= (compile in Compile, sources in Compile, streams) map { (_, sourceFiles, s) =>
    val logger = s.log
    val (feedback, score) = StyleChecker.assess(sourceFiles)
    logger.info(feedback)
    logger.info(s"Style Score: $score out of ${StyleChecker.maxResult}")
  }

  lazy val mongoScalaDriver = Project(
    id = "mongo-scala-driver",
    base = file("driver")
  ).configs(IntTest)
    .configs(AccTest)
    .configs(UnitTest)
    .configs(PerfTest)
    .settings(buildSettings: _*)
    .settings(consoleSettings: _*)
    .settings(docSettings: _*)
    .settings(testSettings: _*)
    .settings(styleCheckSetting: _*)
    .settings(scalaStyleSettings: _*)
    .settings(publishSettings: _*)
    .settings(assemblyJarSettings: _*)
}
