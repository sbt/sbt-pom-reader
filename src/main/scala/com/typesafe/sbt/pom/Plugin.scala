package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{Model => PomModel}
import Project.Initialize

object PomReaderPlugin extends Plugin {
  import SbtPomKeys._
  import MavenHelper._
  
  val useMavenPom: Seq[Setting[_]] = Seq(
    pomLocation <<= baseDirectory apply (_ / "pom.xml"),
    mvnLocalRepository := (file(sys.props("user.home")) / ".m2" / "repository"),
    effectivePom <<= (mvnLocalRepository, pomLocation) apply loadEffectivePom
  ) ++ pullSettingsFromPom

}