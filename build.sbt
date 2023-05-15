ThisBuild / organization := "com.github.sbt"
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))
ThisBuild / developers := List(Developer("", "", "", url("https://github.com/sbt/sbt-pom-reader/graphs/contributors")))
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-pom-reader"))
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / version := {
  val orig = (ThisBuild / version).value
  if (orig.endsWith("-SNAPSHOT")) "2.2.0-SNAPSHOT"
  else orig
}

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

val mvnVersion = "3.8.2"
val mvnResolverVersion = "1.7.2"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    name := "sbt-pom-reader"

    libraryDependencies ++= Seq(
      "org.apache.maven" % "maven-embedder" % mvnVersion
    ) ++ Seq(
      "org.apache.maven.resolver" % "maven-resolver-connector-basic",
      "org.apache.maven.resolver" % "maven-resolver-transport-file",
      "org.apache.maven.resolver" % "maven-resolver-transport-http",
      "org.apache.maven.resolver" % "maven-resolver-transport-wagon"
    ).map (_ % mvnResolverVersion)

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
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.18"
        case "2.12" => "1.2.8"
      }
    }
  })
