package org.scalasbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{
  Model => PomModel,
  Plugin => PomPlugin,
  Dependency => PomDependency
}
import Project.Initialize
import SbtPomKeys._
import collection.JavaConverters._

/** Helper object to extract maven settings. */
object MavenHelper {
  
  def pullSettingsFromPom: Seq[Setting[_]] = Seq(
    name <<= fromPom(_.getArtifactId),
    organization <<= fromPom(_.getGroupId),
    version <<= fromPom(_.getVersion),
    // TODO - Add configuration on whetehr we force the scalaVersion to exist...
    scalaVersion <<= fromPom(getScalaVersionForced),
    libraryDependencies <++= fromPom(getDependencies),
    resolvers <++= fromPom(getResolvers),
    scalacOptions <++= (effectivePom) map { pom =>
      getScalacOptions(pom)
    }
  )
  
  def fromPom[T](f: PomModel => T): Initialize[T] =
    effectivePom apply f
    
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
    
  
  def convertDep(dep: PomDependency): sbt.ModuleID = {
    // TODO - Handle mapping all the maven oddities into sbt's DSL.
    val scopeString: Option[String] = 
      for {
        scope <- Option(dep.getScope)
      } yield scope
      
    def fixScope(dep: ModuleID): ModuleID =
      scopeString match {
        case Some(scope) => dep % scope
        case None => dep
      }
    fixScope(dep.getGroupId % dep.getArtifactId % dep.getVersion)
  }
    
  def getDependencies(pom: PomModel): Seq[ModuleID] = {
    for {
      dep <- pom.getDependencies.asScala
    } yield convertDep(dep)
  }
   
  def getResolvers(pom: PomModel): Seq[Resolver] = {
    for {
      repo <- pom.getRepositories.asScala
      // TODO - Support other layouts
      if repo.getLayout == "default"
    } yield repo.getId at repo.getUrl
  }

  
  // TODO - Pull credentials from ~/.m2/settings.xml
  // TODO - Pull source/resource directories from pom...
  
  // TODO - compiler plugins...
  
}