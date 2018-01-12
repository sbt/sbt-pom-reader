import sbt._

object Dependencies {

  val mvnVersion = "3.5.2"
  val mvnWagonVersion = "2.12"
  val mvnResolver = "1.1.0"

  // val mvnAether           = "org.apache.maven" % "maven-resolver-provider" % mvnVersion
  // val resolverApi         = "org.apache.maven.resolver" % "maven-resolver-api" % mvnResolver
  // val resolverConnBasic   = "org.apache.maven.resolver" % "maven-resolver-connector-basic" % mvnResolver
  // val resolverSpi         = "org.apache.maven.resolver" % "maven-resolver-spi" % mvnResolver
  val transportWagon      = "org.apache.maven.resolver" % "maven-resolver-transport-wagon" % mvnResolver
  // val transportHttp       = "org.apache.maven.resolver" % "maven-resolver-transport-http" % mvnResolver
  val mvnEmbedder         = "org.apache.maven" % "maven-embedder" % mvnVersion

  val mvnWagonLwHttp      = "org.apache.maven.wagon" % "wagon-http-lightweight" % mvnWagonVersion
  val mvnWagonFile        = "org.apache.maven.wagon" % "wagon-file" % mvnWagonVersion
  // val mvnWagon            = "org.apache.maven.wagon" % "wagon-http" % mvnWagonVersion
  // val mvnWagonProviderApi = "org.apache.maven.wagon" % "wagon-provider-api" % mvnWagonVersion


  def pluginDependencies =
     Seq(
      // mvnAether,
      mvnEmbedder,
      // mvnWagon,
      mvnWagonFile,
      mvnWagonLwHttp,
      // mvnWagonProviderApi,
      // resolverApi,
      // resolverConnBasic,
      // resolverSpi,
      transportWagon,
      // transportHttp,
    )
}
