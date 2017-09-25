package com.typesafe.sbt.pom

import org.apache.maven.model.{Model ⇒ PomModel, Repository ⇒ PomRepository}
import org.apache.maven.settings.building.{DefaultSettingsBuilderFactory, DefaultSettingsBuildingRequest}
import org.apache.maven.settings.{Settings ⇒ MavenSettings}
import sbt._

import scala.collection.JavaConverters._

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

  /** Extract any resolvers defined through the user settings.xml file. */
  def getUserResolvers(settings: MavenSettings): Seq[Resolver] = {
    val profiles = settings.getProfilesAsMap
    for {
      profileName ← settings.getActiveProfiles.asScala
      profile ← Option(profiles.get(profileName)).toSeq
      repo ← profile.getRepositories.asScala
    } yield repo.getId at repo.getUrl
  }

  /** Extract the server credentials from the given settings file. */
  def serverCredentials(settings: MavenSettings): Seq[ServerCredentials] = {
    for {
      s ← settings.getServers.asScala
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
