package sbtpomreader

import sbt._
import org.apache.maven.model.Model
import org.apache.maven.settings.{Settings ⇒ MavenSettings}

/** Public plugin keys. */
trait SbtPomKeys {
  lazy val pomLocation = SettingKey[File]("mvn-pom-location", "The location where we can find a pom file.")
  lazy val settingsLocation = SettingKey[File]("mvn-settings-location", "The location of the user settings file. Defaults to `~/.m2/settings.xml`")
  lazy val profiles = SettingKey[Seq[String]]("mvn-profiles", "List of maven profiles to be applied.")
  lazy val mvnLocalRepository = SettingKey[File]("mvn-local-repository", "The location of the maven local repository we can use to cache artifacts.")
  lazy val effectivePom = SettingKey[Model]("mvn-effective-pom", "Reads the maven effective pom.")
  lazy val effectiveSettings = SettingKey[Option[MavenSettings]]("mvn-effective-settings", "The effective maven settings model.")
  lazy val showEffectivePom = TaskKey[Unit]("show-effective-pom", "Displays the effective pom from maven.")
  lazy val mavenUserProperties = SettingKey[Map[String, String]]("maven-user-properties","A map of user properties to be applied")
  lazy val isJavaOnly = SettingKey[Boolean]("is-java-only", "Tells if it is a java only project.")
}

object SbtPomKeys extends SbtPomKeys
