package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{
  Model => PomModel,
  Dependency => PomDependency,
  Repository => PomRepository
}
import org.apache.maven.settings.{Settings ⇒ MavenSettings}
import SbtPomKeys._
import collection.JavaConverters._
import SbtMavenUserSettingsHelper._
import com.typesafe.pom._, MavenHelper._, MavenUserSettingsHelper._

/** Helper object to extract maven settings. */
object SbtMavenHelper {
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
          log.warn("Unable to determine scala version in: " + pomLocation.value + ", using " + scalaVersion.value)
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
