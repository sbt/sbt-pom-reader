val mvnVersion = "3.5.2"
val mvnResolver = "1.1.0"
// These were explicitly added to resolve dependency conflicts in
// maven-embedder. Please keep these up-to-date, with the goal of
// eventually removing them.
val mvnEmbedderDeps = Seq(
  "com.google.guava" % "guava" % "20.0",
  "org.codehaus.plexus" % "plexus-utils" % "3.1.0"
)

ThisBuild  / organization := "com.typesafe.sbt"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / version := {
  val orig = (ThisBuild / version).value
  if (orig.endsWith("-SNAPSHOT")) "2.1.1-SNAPSHOT"
  else orig
}

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(nocomma {
    licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
    name := "sbt-pom-reader"
    pluginCrossBuild / sbtVersion := "1.2.8"
    libraryDependencies ++= Seq(
      "org.apache.maven" % "maven-embedder" % mvnVersion exclude("com.google.guava", "guava") exclude("org.codehaus.plexus", "plexus-utils"),
      "org.apache.maven.resolver" % "maven-resolver-connector-basic" % mvnResolver,
      "org.apache.maven.resolver" % "maven-resolver-transport-file" % mvnResolver,
      "org.apache.maven.resolver" % "maven-resolver-transport-http" % mvnResolver,
      "org.apache.maven.resolver" % "maven-resolver-transport-wagon" % mvnResolver,
    ) ++ mvnEmbedderDeps
    console / initialCommands :=
      """| import com.typesafe.sbt.pom._
         | import sbt._
         | val localRepo = file(sys.props("user.home")) / ".m2" / "repository"
         | val pomFile = file("src/sbt-test/simple-pom/can-extract-basics/pom.xml")
         | val pom = loadEffectivePom(pomFile, localRepo, Seq.empty, Map.empty)
         |""".stripMargin
    scriptedLaunchOpts := scriptedLaunchOpts.value ++ Seq("-Dproject.version=" + version.value)
    bintrayOrganization := Some("sbt")
    bintrayRepository := "sbt-plugin-releases"
    // override sbt-ci-release
    publishMavenStyle := false
    publishTo := (bintray / publishTo).value
  })
