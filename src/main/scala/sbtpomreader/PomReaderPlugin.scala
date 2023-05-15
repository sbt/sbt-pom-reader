package sbtpomreader

import sbt._
import sbt.plugins.JvmPlugin

/** Plugin definition for Maven POM reader. */
object PomReaderPlugin extends AutoPlugin {
  object autoImport extends SbtPomKeys {}
  override def requires = JvmPlugin
  override def trigger = allRequirements
  override lazy val projectSettings = MavenHelper.useMavenPom
}
