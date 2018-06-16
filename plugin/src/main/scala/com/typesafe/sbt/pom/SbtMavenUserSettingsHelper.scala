package com.typesafe.sbt.pom

import org.apache.maven.model.{Model ⇒ PomModel, Repository ⇒ PomRepository}
import org.apache.maven.settings.building.{DefaultSettingsBuilderFactory, DefaultSettingsBuildingRequest}
import org.apache.maven.settings.{Settings ⇒ MavenSettings}
import sbt._

import scala.collection.JavaConverters._

/** Helper object with functions to extract settings from the user's
  * Maven settings file (typically ~/.m2/settings.xml) */
object SbtMavenUserSettingsHelper {
  /** Extract any resolvers defined through the user settings.xml file. */
  def getUserResolvers(settings: MavenSettings): Seq[Resolver] = {
    val profiles = settings.getProfilesAsMap
    for {
      profileName ← settings.getActiveProfiles.asScala
      profile ← Option(profiles.get(profileName)).toSeq
      repo ← profile.getRepositories.asScala
    } yield repo.getId at repo.getUrl

  }
}
