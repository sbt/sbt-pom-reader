package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{Model => PomModel}
import Project.Initialize

/** A helper class that allows us to load all maven reactor projects
 *  upon boot.
 */
trait PomBuild extends Build {
  import SbtPomKeys._
  import MavenHelper._
  val profiles: Seq[String] = Seq()

  lazy val overrideRootProjectName:Option[String] = None
  override def projectDefinitions(baseDirectory: File): Seq[Project] = {
    // If we detect a maven parent pom, use it.
    if((baseDirectory / "pom.xml").exists)
      MavenProjectHelper.makeReactorProject(baseDirectory, overrideRootProjectName, profiles)
    else super.projectDefinitions(baseDirectory)
  }
}
