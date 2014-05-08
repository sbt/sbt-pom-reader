package com.typesafe.sbt.pom

import sbt._
import org.apache.maven.model.Model

object SbtPomKeys {
  val pomLocation = SettingKey[File]("mvn-pom-location", "The location where we can find a pom file.")
  val profiles = SettingKey[Seq[String]]("mvn-profiles", "List of maven profiles to be applied.")
  val mvnLocalRepository = SettingKey[File]("mvn-local-repository", "The location of the maven local repository we can use to cache artifacts.")
  val effectivePom = SettingKey[Model]("mvn-effective-pom", "Reads the maven effective pom.")
  val showEffectivePom = TaskKey[Unit]("show-effective-pom", "Displays the effective pom from maven.")
}