import sbt._

object Dependencies {
  // Versions
  val scalaCoreVersion     = "2.11.0-RC1"

  val mongodbDriverVersion = "3.0.0-SNAPSHOT"
  val rxJavaScalaVersion   = "0.17.0-RC7"

  val scalaTestVersion     = "2.1.0"
  val scalaMeterVersion    = "0.5-SNAPSHOT"


  // Scala
  val scalaReflection    = "org.scala-lang" % "scala-reflect" % scalaCoreVersion
  val scalaCompiler      = "org.scala-lang" % "scala-compiler" % scalaCoreVersion

  // Libraries
  val mongodbDriver = "org.mongodb" % "mongodb-driver" % mongodbDriverVersion
  val rxJavaScala   = "com.netflix.rxjava" % "rxjava-scala" % rxJavaScalaVersion

  // Testing Libraries
  val scalaTest     = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

  // Todo - waiting for 2.11.0-RC1 version
  // val scalaMeter    = "com.github.axel22" % "scalameter_2.11.0-RC1" % scalaMeterVersion % "test"

  // Projects
  val coreDependencies = Seq(scalaCompiler, scalaReflection, mongodbDriver, rxJavaScala)
  val testDependencies = Seq(scalaTest)
}
