import sbt._

object Dependencies {

  val mvnVersion = "3.0.4"

  //val aether         = "org.sonatype.aether" % "aether" % "1.13.1"
  val mvnAether      = "org.apache.maven" % "maven-aether-provider" % mvnVersion
  val aetherWagon    = "org.sonatype.aether" % "aether-connector-wagon" % "1.13.1"
  val mvnWagon    = "org.apache.maven.wagon" % "wagon-http" % "2.2"
  val mvnEmbedder    = "org.apache.maven" % "maven-embedder" % mvnVersion


  def pluginDependencies =
     Seq(mvnAether, aetherWagon, mvnWagon, mvnEmbedder)
}
