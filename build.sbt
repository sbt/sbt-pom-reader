name := "sbt-pom-reader"

organization := "com.typesafe.sbt"

sbtPlugin := true

publishMavenStyle := false

libraryDependencies ++= Dependencies.pluginDependencies

git.baseVersion := "1.0"

versionWithGit

scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }

initialCommands :=
  """| import com.typesafe.sbt.pom._
     | import sbt._
     | val localRepo = file(sys.props("user.home")) / ".m2" / "repository"
     | val pom = loadEffectivePom(localRepo, file("src/sbt-test/simple-pom/can-extract-basics/pom.xml"))
     |""".stripMargin


scriptedSettings

scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }
