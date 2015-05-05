package com.typesafe.sbt.pom

import sbt._

/** sbt plugin definition for Maven POM reader. */
object PomReaderPlugin extends AutoPlugin {
  object autoImport extends SbtPomKeys
  override def trigger = allRequirements
  override lazy val projectSettings = MavenHelper.useMavenPom

  @deprecated("This is an AutoPlugin; settings are automatically added. Using this value is no longer required", "1.1")
  lazy val useMavenPom = projectSettings
}
