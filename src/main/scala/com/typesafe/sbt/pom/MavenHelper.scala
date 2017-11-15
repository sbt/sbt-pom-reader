package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{
  Model => PomModel,
  Plugin => PomPlugin,
  Dependency => PomDependency,
  Repository => PomRepository
}
import org.apache.maven.settings.{Settings ⇒ MavenSettings}
import SbtPomKeys._
import collection.JavaConverters._
import MavenUserSettingsHelper._
import scala.util.Try

/** Helper object to extract maven settings. */
object MavenHelper {
  
  // Load pom values into settings.
  val useMavenPom: Seq[Setting[_]] =
    loadPomInSettings ++ pullSettingsFromPom

  def loadPomInSettings: Seq[Setting[_]]= Seq(
    pomLocation := baseDirectory.value / "pom.xml",
    settingsLocation := file(sys.props("user.home")) / ".m2" / "settings.xml",
    mvnLocalRepository := defaultLocalRepo,
    profiles := Seq.empty,
    mavenUserProperties := Map.empty,
    effectivePom := loadEffectivePom(pomLocation.value, mvnLocalRepository.value, profiles.value, mavenUserProperties.value),
    effectiveSettings := loadUserSettings(settingsLocation.value, profiles.value),
    showEffectivePom := showPom(pomLocation.value, effectivePom.value, streams.value),
    isJavaOnly := false
  )
  

  // We Synchronize on System.out so aggregate logs don't interleave....
  def showPom(location: File, model: PomModel, s: TaskStreams): Unit = System.out.synchronized {
    s.log.info("---- Effective pom ("+location+") ----")
    serializePom(model).split("[\r\n]+") foreach { line =>
      s.log.info(line)  
    }
  }
  
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

  def pullSettingsFromPom: Seq[Setting[_]] = Seq(
    /* Often poms have artifactId with binary version suffix. This should ideally be removed. */
    name := fromPom(x => removeBinaryVersionSuffix(x.getArtifactId)).value,
    organization := fromPom(_.getGroupId).value,
    version := fromPom(_.getVersion).value,
    // TODO - Add configuration on whether we force the scalaVersion to exist...
    scalaVersion := {
      val log = sLog.value
      getScalaVersion(effectivePom.value) getOrElse {
        if (!isJavaOnly.value)
          log.warn("Unable to determine scala version in: " + pomLocation.value + ", using " + scalaVersion)
        scalaVersion.value
      }
    },

    unmanagedSourceDirectories in Compile ++= {
      getAdditionalSourcesFromPlugin(effectivePom.value).filterNot(_.contains("test")).map(x => baseDirectory.value / x)
    },

    unmanagedSourceDirectories in Test ++= {
      getAdditionalSourcesFromPlugin(effectivePom.value).filter(_.contains("test")).map(x => baseDirectory.value / x)
    },

    libraryDependencies ++= fromPom(getDependencies).value,
    resolvers ++= {
      val pr = getPomResolvers(effectivePom.value)
      val sr = effectiveSettings.value.map(getUserResolvers).getOrElse(Seq.empty)
      pr ++ sr
    },
    // TODO - split into Compile/Test/Runtime/Console
    scalacOptions ++= {
      getScalacOptions(effectivePom.value)
    },
    credentials ++= createSbtCredentialsFromUserSettings(effectivePom.value, effectiveSettings.value)
  )
  
  def fromPom[T](f: PomModel => T): Def.Initialize[T] =
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
    
  
  def convertDep(dep: PomDependency): sbt.ModuleID = {
    // TODO - Handle mapping all the maven oddities into sbt's DSL.
    val scopeString: Option[String] = 
      for {
        scope <- Option(dep.getScope)
      } yield scope
      
    def fixScope(m: ModuleID): ModuleID =
      scopeString match {
        case Some(scope) => m % scope
        case None => m
      }
      
    def addExclusions(mod: ModuleID): ModuleID = {
      val exclusions = dep.getExclusions.asScala
      exclusions.foldLeft(mod) { (mod, exclude) =>
        mod.exclude(exclude.getGroupId, exclude.getArtifactId)
      }
    }
    def addClassifier(mod: ModuleID): ModuleID = {
      Option(dep.getClassifier) match {
        case Some(_classifier) => mod classifier _classifier
        case None => mod
      }
    }
    addExclusions(addClassifier(fixScope(dep.getGroupId % dep.getArtifactId % dep.getVersion)))
  }
    
  def getDependencies(pom: PomModel): Seq[ModuleID] = {
    for {
      dep <- pom.getDependencies.asScala
    } yield convertDep(dep)
  }
   
  def getPomResolvers(pom: PomModel): Seq[Resolver] = {
    for {
      repo <- pom.getRepositories.asScala
      // TODO - Support other layouts
      if repo.getLayout == "default"
    } yield repo.getId at repo.getUrl
  }

  /** Attempts to perform an unathorized action so we can detect the supported
    * authentication realms of our server.  tested against nexus + artifactory. */
  def getServerRealm(method: String, uri: String): Option[String] = {
    // This is consigned to a Try until proper handling of offline mode.
    Try {
      val con = url(uri).openConnection.asInstanceOf[java.net.HttpURLConnection]
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
    url(uri).getHost
  
  def makeSbtCredentials(creds: Seq[(PomRepository, ServerCredentials)]) =
    for {
      (repo, cred) <- creds
      // If we can't find the realm, just hack in the two we know most people use.
      realm <- getServerRealmSafe(repo.getUrl).map(Nil.::).getOrElse(Seq("Artifactory Realm","Sonatype Nexus Repository Manager"))
      host = getHost(repo.getUrl)
    } yield Credentials(realm, host, cred.user, cred.pw)
    
    
  def createSbtCredentialsFromUserSettings(pom: PomModel, effectiveSettings: Option[MavenSettings]): Seq[Credentials] = {
    for {
      settings ← effectiveSettings
      creds = serverCredentials(settings)
      matched = matchCredentialsWithServers(creds, pom)
    } yield makeSbtCredentials(matched)
  } getOrElse(Seq.empty)
  
  // TODO - Pull resource directories from pom...

  // TODO - compiler plugins...
  
}
