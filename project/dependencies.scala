import sbt._

object Dependencies {

  val mvnVersion = "3.5.2"
  val mvnWagonVersion = "2.12"
  val mvnResolver = "1.1.0"

  val transportWagon      = "org.apache.maven.resolver" % "maven-resolver-transport-wagon" % mvnResolver
  val mvnEmbedder         = "org.apache.maven" % "maven-embedder" % mvnVersion

  val mvnWagonLwHttp      = "org.apache.maven.wagon" % "wagon-http-lightweight" % mvnWagonVersion
  val mvnWagonFile        = "org.apache.maven.wagon" % "wagon-file" % mvnWagonVersion


  def pluginDependencies =
     Seq(
      mvnEmbedder,
      mvnWagonFile,
      mvnWagonLwHttp,
      transportWagon,
    )
}
