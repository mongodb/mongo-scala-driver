import sbt._

object Resolvers {
  // Repositories
  val sonatypeSnaps = "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val sonatypeRels  = "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

  val typeSafeSnaps = "TypeSafe snapshots" at "http://repo.typesafe.com/typesafe/snapshots"
  val typeSafeRels  = "TypeSafe releases" at "http://repo.typesafe.com/typesafe/releases"

  val localMaven    = "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

  val mongoScalaResolvers = Seq(localMaven, sonatypeSnaps, sonatypeRels, typeSafeSnaps, typeSafeRels)
}
