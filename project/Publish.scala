import sbt._
import sbt.Keys._
import sbt.Def.Initialize
import scala.xml.{NodeSeq, NodeBuffer}


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
