package com.typesafe.sbt.pom

import com.typesafe.sbt.pom.SbtPomKeys._
import org.apache.maven.model.{Model ⇒ PomModel, Repository ⇒ PomRepository}
import org.apache.maven.settings.{Settings ⇒ MavenSettings}
import org.apache.maven.settings.building.{DefaultSettingsBuildingRequest, DefaultSettingsBuilderFactory}
import sbt.Project._
import sbt._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/** Helper object with functions to extract settings from the user's
  * Maven settings file (typically ~/.m2/settings.xml) */
object MavenUserSettingsHelper {
  /** Container for server credentials saved in the global settings. */
  case class ServerCredentials(id: String, user: String, pw: String)

  /** Load the effective maven settings. */
  def loadUserSettings(settingsFile: File, profiles: Seq[String]): Option[MavenSettings] = {
    if (settingsFile.exists()) {
      val settingsBuilder = new DefaultSettingsBuilderFactory().newInstance
      val request = new DefaultSettingsBuildingRequest
      request setUserSettingsFile settingsFile
      val userSettings = settingsBuilder.build(request).getEffectiveSettings
      profiles.foreach(userSettings.addActiveProfile)
      Some(userSettings)
    } else None
  }

  def getUserResolvers(settings: MavenSettings): Seq[Resolver] = {
    val profiles = settings.getProfilesAsMap
    for {
      profileName ← settings.getActiveProfiles
      profile = profiles.get(profileName)
      repo ← profile.getRepositories
    } yield repo.getId at repo.getUrl
  }

  /** Extract the server credentials from the given settings file. */
  def serverCredentials(settings: MavenSettings): Seq[ServerCredentials] = {
    for {
      s ← settings.getServers
    } yield ServerCredentials(s.getId, s.getUsername, s.getPassword)
  }

  /** Associates server credentials defined in the settings with repositories referenced in the POM. */
  // TODO - Grab authentication realm...
  def matchCredentialsWithServers(creds: Seq[ServerCredentials], pom: PomModel): Seq[(PomRepository, ServerCredentials)] = {
    // TODO: handle repos in settings.xml
    for {
      repo <- pom.getRepositories.asScala
      cred <- creds
      if cred.id == repo.getId
    } yield (repo -> cred)
  }
}
