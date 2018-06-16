package com.typesafe.pom

import java.net.URL

import org.apache.maven.model.{Dependency => PomDependency, Model => PomModel, Plugin => PomPlugin, Repository => PomRepository}

import collection.JavaConverters._
import scala.util.Try

object MavenHelper {
  def serializePom(pom: PomModel): String = {
    val out = new java.io.StringWriter
    val writer = new org.apache.maven.model.io.xpp3.MavenXpp3Writer
    try writer.write(out, pom)
    catch {
      case e: Throwable =>
        e.printStackTrace()
        throw e
    }
    finally out.close()
    out.getBuffer.toString
  }
  val ExtractIdRegex = """(.*)_2.\d+.*""".r
  def removeBinaryVersionSuffix(v: String): String = v match {
    case ExtractIdRegex(a) => a
    case _ => v
  }

  // TODO - Use the sbt setting to configure this one...
  val supportedScalaGroupId = "org.scala-lang"
  val supportedScalaArtifacts =
    Seq("scala-library", "scala-library-all", "scala-actors", "scala-swing")
  def getScalaVersionFromDependencies(pom: PomModel): Option[String] = {
    pom.getDependencies.asScala find { dep =>
      (dep.getGroupId  == supportedScalaGroupId) &&
        (supportedScalaArtifacts contains dep.getArtifactId)
    } map (_.getVersion)
  }

  val scalaMavenPluginGroup = "net.alchim31.maven"
  val scalaMavenPluginId = "scala-maven-plugin"

  // TODO - we need to detect which *variant* of a scala plugin is used,
  // whether it's confiugred for compiling/running/testing etc.
  def getScalaPlugin(pom: PomModel): Option[PomPlugin] = {
    pom.getBuild.getPlugins.asScala find { plugin =>
      (plugin.getGroupId == scalaMavenPluginGroup) &&
        (plugin.getArtifactId == scalaMavenPluginId)
    }
  }

  def getAdditionalSourcesPlugin(pom: PomModel): Seq[PomPlugin] = {
    pom.getBuild.getPlugins.asScala filter { plugin =>
      (plugin.getGroupId == "org.codehaus.mojo") &&
        (plugin.getArtifactId == "build-helper-maven-plugin")
    }
  }

  def getAdditionalSourcesFromPlugin(pom: PomModel): Seq[String] = {
    val additionalSources = for {
      plugin <- getAdditionalSourcesPlugin(pom) // TODO: Support more plugins
      config <- plugin.getExecutions.iterator.asScala.map(_.getConfiguration)
      sources <- readAdditionalSourcesPlugin(config)
    } yield sources
    // `asInstanceOf` bit is to help the presentation compiler along...
    additionalSources.flatten.asInstanceOf[Seq[String]].
      filterNot(x => x.trim == "src/main/scala" || x.trim == "src/test/scala")
  }

  def readAdditionalSourcesPlugin(config: java.lang.Object): Option[Seq[String]] =
    for {
      dom <- domOrNone(config)
      srcs <- Option(dom getChild "sources")
    } yield srcs.getChildren map (_.getValue)

  def domOrNone(config: java.lang.Object): Option[org.codehaus.plexus.util.xml.Xpp3Dom] =
    if(config.isInstanceOf[org.codehaus.plexus.util.xml.Xpp3Dom]) {
      Some(config.asInstanceOf[org.codehaus.plexus.util.xml.Xpp3Dom])
    } else None

  def readScalaPluginConfigurationFor(config: java.lang.Object, elem: String): Option[String] =
    for {
      dom <- domOrNone(config)
      child <- Option(dom.getChild(elem))
    } yield child.getValue
  def getScalaVersionFromPlugins(pom: PomModel): Option[String] =
    for {
      plugin <- getScalaPlugin(pom)
      config <- Option(plugin.getConfiguration)
      version <- readScalaPluginConfigurationFor(config, "scalaVersion")
    } yield version

  def readScalaPluginScalacArgsConfig(config: java.lang.Object): Option[Seq[String]] =
    for {
      dom <- domOrNone(config)
      args <- Option(dom getChild "args")
    } yield  args.getChildren map (_.getValue)

  def getScalacOptions(pom: PomModel): Seq[String] = {
    val discovered =
      for {
        plugin <- getScalaPlugin(pom)
        config <- Option(plugin.getConfiguration)
        opts <- readScalaPluginScalacArgsConfig(config)
      } yield opts
    discovered getOrElse Nil
  }

  def getScalaVersion(pom: PomModel): Option[String] =
    getScalaVersionFromDependencies(pom) orElse
      getScalaVersionFromPlugins(pom)

  def getScalaVersionForced(pom: PomModel): String =
    getScalaVersion(pom) getOrElse
      sys.error("Could not find scala version in pom file.  Please depend on scala-library directly.")

  /** Attempts to perform an unathorized action so we can detect the supported
    * authentication realms of our server.  tested against nexus + artifactory. */
  def getServerRealm(method: String, uri: String): Option[String] = {
    // This is consigned to a Try until proper handling of offline mode.
    Try {
      val con = new URL(uri).openConnection.asInstanceOf[java.net.HttpURLConnection]
      con setRequestMethod method
      if (con.getResponseCode == 401) {
        val authRealmConfigs = con.getHeaderField("WWW-Authenticate")
        val BasicRealm = new scala.util.matching.Regex(
          """.*[Bb][Aa][Ss][Ii][Cc]\s+[Rr][Ee][Aa][Ll][Mm]\=\"(.*)\".*""")
        // Artifactory appears not to ask for authentication realm,but nexus does immediately.
        BasicRealm.unapplySeq(authRealmConfigs) flatMap (_.headOption)
      }
      else None
    } getOrElse(None)
  }

  def getServerRealmSafe(uri: String): Option[String] = {
    // We have to try both PUT and POST because of Nexus vs. Artifactory differences on when they
    // report authentication realm issues.
    getServerRealm("PUT", uri) orElse
      getServerRealm("POST", uri)
  }

  def getHost(uri: String): String =
    new URL(uri).getHost

}
