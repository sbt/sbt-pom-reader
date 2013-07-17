package com.typesafe.sbt.pom

import sbt._
import org.apache.maven.model.Model

object SbtPomKeys {
  val pomLocation = SettingKey[File]("mvn-pom-location", "The location where we can find a pom file.")
  val mvnLocalRepository = SettingKey[File]("mvn-local-repository", "The location of the maven local repository we can use to cache artifacts.")
  val effectivePom = SettingKey[Model]("mvn-effective-pom", "Reads the maven effective pom.")
}