package com.typesafe.sbt.pom

import sbt._

/** A helper class that allows us to load all maven reactor projects
 *  upon boot.
 */
trait PomBuild extends Build {
  val profiles: Seq[String] = Seq()

  /** These can be used to override properties in maven pom */
  val userPropertiesMap: Map[String, String] = Map.empty

  override def settings = {
    super.settings ++ Seq(SbtPomKeys.profiles := profiles, SbtPomKeys.mavenUserProperties := userPropertiesMap)
  }

  lazy val overrideRootProjectName:Option[String] = None
  override def projectDefinitions(baseDirectory: File): Seq[Project] = {
    // If we detect a maven parent pom, use it.
    if((baseDirectory / "pom.xml").exists)
      MavenProjectHelper.makeReactorProject(baseDirectory, overrideRootProjectName, profiles, userPropertiesMap)
    else super.projectDefinitions(baseDirectory)
  }
}
