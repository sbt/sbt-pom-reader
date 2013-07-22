package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{
  Model => PomModel,
  Plugin => PomPlugin,
  Dependency => PomDependency,
  Repository => PomRepository
}
import Project.Initialize
import SbtPomKeys._
import collection.JavaConverters._

/** Helper object to extract maven settings. */
object MavenHelper {
  
  def makeReactorProject(baseDir: File): Seq[Project] = {
    // TODO - Wire dependencies
    for {
      (pom, pomFile) <- allProjectsAndDirectories(baseDir)
      name = makeProjectName(pom)
      
    } yield (
      // TODO - Figure out how to determine dependencies on projects...
      sbt.Project(name, pomFile.getParentFile)
      settings(useMavenPom:_*)
    )
  }
  
  def makeProjectName(pom: PomModel): String = 
    Option(pom.getName) getOrElse pom.getArtifactId
  
  def allProjectsAndDirectories(base: File): Seq[(PomModel, File)] = {
    println("Looking for reactor projects in: " + base)
    val pom = base / "pom.xml"
    @annotation.tailrec
    def grabAllProjects(toCheck: List[File], all: Seq[(PomModel,File)] = Nil): Seq[(PomModel, File)] = 
      toCheck match {
        case next :: rest =>
          println("Checking pom file: " + next)
          val pom = loadEffectivePom(next)
          val children = getChildProjectPoms(pom, next)
          // Update what we need to do next
          val nextToCheck = rest ++ children
          val nextAll = all :+ (pom, next)
          grabAllProjects(nextToCheck, nextAll)
        case Nil => all
      }
    grabAllProjects(pom :: Nil)
  }
  
  def getChildProjectPoms(pom: PomModel, pomFile: File): Seq[File] =
    for {
      childDirName <- Option(pom.getModules) map (_.asScala) getOrElse Nil
      childPom = pomFile.getParentFile / childDirName / "pom.xml"
      if childPom.exists
    } yield childPom
  
  
  // Load pom values into settings.
  lazy val useMavenPom: Seq[Setting[_]] =
    loadPomInSettings ++ pullSettingsFromPom
  
  def loadPomInSettings: Seq[Setting[_]]= Seq(
    pomLocation <<= baseDirectory apply (_ / "pom.xml"),
    mvnLocalRepository := defaultLocalRepo,
    effectivePom <<= (pomLocation, mvnLocalRepository) apply loadEffectivePom
  )
  
  def pullSettingsFromPom: Seq[Setting[_]] = Seq(
    name <<= fromPom(_.getArtifactId),
    organization <<= fromPom(_.getGroupId),
    version <<= fromPom(_.getVersion),
    // TODO - Add configuration on whether we force the scalaVersion to exist...
    scalaVersion <<= fromPom(getScalaVersionForced),
    libraryDependencies <++= fromPom(getDependencies),
    resolvers <++= fromPom(getResolvers),
    scalacOptions <++= (effectivePom) map { pom =>
      getScalacOptions(pom)
    },
    credentials <++= effectivePom map createSbtCredentialsFromSettingsXml
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
  def settingsFile = file(sys.props("user.home")) / ".m2" / "settings.xml"
  
  def settingsXml: scala.xml.Node =
    sbt.Using.fileInputStream(settingsFile) { in =>
      scala.xml.XML.load(in)
    }
  case class ServerCredentials(id: String, user: String, pw: String)
  def parseServersFromSettings(xml: scala.xml.Node): Seq[ServerCredentials] = {
    val servers = xml \ "servers" \\ "server"
    // TODO - Support alternative layouts here...
    val result = 
      for(server <- servers) yield {
        val id = (server \ "id").text
        val user = (server \ "username").text
        val pw = (server \ "password").text
        ServerCredentials(id, user, pw)
      }
    result filterNot { x =>
      x.user.isEmpty || x.pw.isEmpty  
    }
  }
  def settingsXmlServers: Seq[ServerCredentials] = 
    parseServersFromSettings(settingsXml)
  
  // TODO - Grab authentication realm...
  def matchCredentialsWithServers(creds: Seq[ServerCredentials], pom: PomModel): Seq[(PomRepository, ServerCredentials)] = {
    for {
      repo <- pom.getRepositories.asScala
      cred <- creds
      if cred.id == repo.getId
    } yield (repo -> cred)
  }
  
  
  // Attempts to perform an unathorized action so we can detect the supported
  // authentication realms of our server.  tested against nexus + artifactory.
  def getServerRealm(method: String, uri: String): Option[String] = {
    val con = url(uri).openConnection.asInstanceOf[java.net.HttpURLConnection]
    con setRequestMethod method
    if(con.getResponseCode == 401) {
      val authRealmConfigs = con.getHeaderField("WWW-Authenticate")
      val BasicRealm = new scala.util.matching.Regex(""".*[Bb][Aa][Ss][Ii][Cc] [Rr][Ee][Aa][Ll][Mm]\=\"(.*)\".*""")
      // Artifactory appears not to ask for authentication realm,but nexus does immediately.
      BasicRealm.unapplySeq(authRealmConfigs) flatMap (_.headOption)
    } else None
  }
  
  
  def getServerRealm(uri: String): String = {
    // We have to try both PUT and POST because of Nexus vs. Artifactory differences on when they
    // report authentication realm issues.
    getServerRealm("PUT", uri) orElse
    getServerRealm("POST", uri) getOrElse 
    sys.error("Unable to determine authentication realm for: " + uri)
  }
  
  def getHost(uri: String): String =
    url(uri).getHost
  
  def makeSbtCredentials(creds: Seq[(PomRepository, ServerCredentials)]) =
    for {
      (repo, cred) <- creds
      realm = getServerRealm(repo.getUrl)
      host = getHost(repo.getUrl)
    } yield Credentials(realm, host, cred.user, cred.pw)
    
    
  def createSbtCredentialsFromSettingsXml(pom: PomModel): Seq[Credentials] = {
    val serverConfig = settingsXmlServers
    val matched = matchCredentialsWithServers(serverConfig, pom)
    makeSbtCredentials(matched)
  }
  // TODO - Pull source/resource directories from pom...
  
  // TODO - compiler plugins...
  
}