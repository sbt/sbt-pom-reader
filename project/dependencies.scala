import sbt._

object Dependencies {

  val mvnVersion = "3.5.2"
  val mvnResolver = "1.1.0"

  val transportWagon      = "org.apache.maven.resolver" % "maven-resolver-transport-wagon" % mvnResolver
  val mvnEmbedder         = "org.apache.maven" % "maven-embedder" % mvnVersion


  def pluginDependencies =
     Seq(
      mvnEmbedder,
      transportWagon,
    )
}
