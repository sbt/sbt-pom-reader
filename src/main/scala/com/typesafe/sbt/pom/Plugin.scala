package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{Model => PomModel}
import Project.Initialize

object PomReaderPlugin extends Plugin {
  val useMavenPom: Seq[Setting[_]] = 
    MavenHelper.useMavenPom
}