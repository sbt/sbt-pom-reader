name := "sbt-pom-reader"

organization := "com.typesafe.sbt"

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

sbtPlugin := true

libraryDependencies ++= Dependencies.pluginDependencies

initialCommands in console :=
  """| import com.typesafe.sbt.pom._
     | import sbt._
     | val localRepo = file(sys.props("user.home")) / ".m2" / "repository"
     | val pomFile = file("src/sbt-test/simple-pom/can-extract-basics/pom.xml")
     | val pom = loadEffectivePom(pomFile, localRepo, Seq.empty, Map.empty)
     |""".stripMargin


scriptedSettings

scriptedLaunchOpts <+= version apply { v => "-Dproject.version=" + v }

