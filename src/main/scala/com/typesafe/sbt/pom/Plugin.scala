package com.typesafe.sbt.pom

import sbt._

/** sbt plugin definition for Maven POM reader. */
object PomReaderPlugin extends Plugin {
  val useMavenPom: Seq[Setting[_]] = MavenHelper.useMavenPom
}
