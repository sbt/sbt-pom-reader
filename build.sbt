val mvnVersion = "3.9.4"
val mvnResolverVersion = "1.9.18"
val scala212 = "2.12.19"

ThisBuild / organization := "com.github.sbt"
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))
ThisBuild / developers := List(Developer("", "", "", url("https://github.com/sbt/sbt-pom-reader/graphs/contributors")))
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-pom-reader"))
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / version := {
  val orig = (ThisBuild / version).value
  if (orig.endsWith("-SNAPSHOT")) "2.5.0-SNAPSHOT"
  else orig
}
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala212)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-pom-reader"
    scalacOptions := Seq(
      "-Wconf:any:wv",
      "-Xlint:unused",
      "-Xlint:deprecation"
    )

    libraryDependencies ++= Seq(
      "org.apache.maven" % "maven-embedder" % mvnVersion
    ) ++ Seq(
      "org.apache.maven.resolver" % "maven-resolver-connector-basic",
      "org.apache.maven.resolver" % "maven-resolver-supplier",
      "org.apache.maven.resolver" % "maven-resolver-transport-file",
      "org.apache.maven.resolver" % "maven-resolver-transport-http",
      "org.apache.maven.resolver" % "maven-resolver-transport-wagon"
    ).map(_ % mvnResolverVersion)

    console / initialCommands :=
      """| import com.typesafe.sbt.pom._
         | import sbt._
         | val localRepo = file(sys.props("user.home")) / ".m2" / "repository"
         | val pomFile = file("src/sbt-test/simple-pom/can-extract-basics/pom.xml")
         | val pom = loadEffectivePom(pomFile, localRepo, Seq.empty, Map.empty)
         |""".stripMargin

    scriptedLaunchOpts := scriptedLaunchOpts.value ++ Seq("-Dproject.version=" + version.value)
    scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)
    scriptedBufferLog := true
    scriptedSbt := "1.9.9"
  })
