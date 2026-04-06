package sbtpomreader

import java.io.File
import java.util.Locale

import scala.collection.JavaConverters._

import org.apache.maven.model.Model
import org.apache.maven.model.building.{
  DefaultModelBuilderFactory,
  DefaultModelBuildingRequest,
  ModelBuildingException,
  ModelBuildingRequest
}
import org.apache.maven.model.resolution.ModelResolver
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.repository.{ MirrorSelector, RemoteRepository }

object MavenPomResolver {
  val system = newRepositorySystemImpl
  require(system != null, "Repository system failed to initialize")

  private[sbtpomreader] def mirrorSelector(settingsFile: File): Option[MirrorSelector] = {
    MavenUserSettingsHelper.loadUserSettings(settingsFile, Seq.empty).flatMap { settings =>
      val selector = MavenUserSettingsHelper.buildMirrorSelector(settings)
      if (settings.getMirrors.isEmpty) None else Some(selector)
    }
  }

  def apply(localRepo: File, settingsFile: File) = new MavenPomResolver(system, localRepo, settingsFile)
}

class MavenPomResolver(system: RepositorySystem, localRepo: File, settingsFile: File) {
  private val mirrors = MavenPomResolver.mirrorSelector(settingsFile)

  val session = {
    val s = newSessionImpl(system, localRepo)
    mirrors.foreach(s.setMirrorSelector)
    s
  }

  private val modelBuilder = (new DefaultModelBuilderFactory).newInstance

  private val defaultRepositories: Seq[RemoteRepository] = {
    val central = new RemoteRepository.Builder(
      "central", "default", "https://repo.maven.apache.org/maven2"
    ).build()
    mirrors match {
      case Some(selector) =>
        Seq(Option(selector.getMirror(central)).getOrElse(central))
      case None =>
        Seq(central)
    }
  }

  // TODO - Add repositories from the pom...
  val modelResolver: ModelResolver = {
    new MavenModelResolver(
      session,
      system,
      repositories = defaultRepositories
    )
  }

  def loadEffectivePom(
      pomFile: File,
      repositories: Seq[RemoteRepository],
      activeProfiles: Seq[String],
      userPropsMap: Map[String, String]
  ): Model =
    try {
      val userProperties = new java.util.Properties()
      userPropsMap.foreach(kv => userProperties.put(kv._1, kv._2))
      val request = new DefaultModelBuildingRequest
      request setLocationTracking true
      request setProcessPlugins false
      request setPomFile pomFile
      request setValidationLevel ModelBuildingRequest.VALIDATION_LEVEL_STRICT
      // TODO - Pass as arguments?
      request setSystemProperties systemProperties
      request setUserProperties userProperties
      request setActiveProfileIds activeProfiles.asJava
      // TODO - Model resolver?
      request setModelResolver modelResolver

      (modelBuilder build request).getEffectiveModel
    } catch {
      case e: ModelBuildingException =>
        // TODO - Wrap in better exception...
        throw e
    }

  lazy val systemProperties = {
    val props = new java.util.Properties
    envProperties.foreach(kv => props.put(kv._1, kv._2))
    System.getProperties().asScala.foreach(kv => props.put(kv._1, kv._2))
    // TODO - Add more?
    props
  }

  lazy val envProperties: Map[String, String] = {
    val caseInsenstive = false // TODO - is windows?
    System.getenv.entrySet.asScala.map { entry =>
      val key = "env." + (if (caseInsenstive) entry.getKey.toUpperCase(Locale.ENGLISH)
                          else entry.getKey)
      key -> entry.getValue
    } { collection.breakOut }
  }
}
