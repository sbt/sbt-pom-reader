import sbt._

object Dependencies {

  val mvnVersion = "3.5.2"
  val mvnResolver = "1.1.0"

  val transportWagon      = "org.apache.maven.resolver" % "maven-resolver-transport-wagon" % mvnResolver
  val mvnEmbedder         = "org.apache.maven" % "maven-embedder" % mvnVersion exclude("com.google.guava", "guava") exclude("org.codehaus.plexus", "plexus-utils")

  // These were explicitly added to resolve dependency conflicts in
  // maven-embedder. Please keep these up-to-date, with the goal of
  // eventually removing them.
  val mvnEmbedderDeps = Seq(
    "com.google.guava" % "guava" % "20.0",
    "org.codehaus.plexus" % "plexus-utils" % "3.1.0"
  )

  def pluginDependencies =
     Seq(
      mvnEmbedder,
      transportWagon,
    ) ++ mvnEmbedderDeps
}
