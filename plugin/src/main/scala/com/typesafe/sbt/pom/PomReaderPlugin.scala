package com.typesafe.sbt.pom

import sbt._
import sbt.plugins.JvmPlugin

/** Plugin definition for Maven POM reader. */
object PomReaderPlugin extends AutoPlugin {
  object autoImport extends SbtPomKeys {
    @deprecated("This is an AutoPlugin; settings are automatically added. Using this value is no longer required", "1.1")
    lazy val useMavenPom = projectSettings
  }
  override def requires = JvmPlugin
  override def trigger = allRequirements
  override lazy val projectSettings = SbtMavenHelper.useMavenPom
}
