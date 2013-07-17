name := "sbt-pom-reader"

organization := "org.scala-sbt.plugins"

sbtPlugin := true

publishMavenStyle := false

libraryDependencies ++= Dependencies.pluginDependencies


scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }

initialCommands :=
  """| import org.scalasbt.pom._
     | import sbt._
     | val localRepo = file(sys.props("user.home")) / ".m2" / "repository"
     | val pom = loadEffectivePom(localRepo, file("src/sbt-test/simple-pom/can-extract-basics/pom.xml"))
     |""".stripMargin


scriptedSettings

scriptedLaunchOpts <+= version apply { v => "-Dproject.version="+v }
