import com.typesafe.sbt.pom.SbtPomKeys.settingsLocation

useMavenPom

settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("check-settings") <<= state map { s =>
  val extracted = Project extract s
  val (_, creds) = extracted runTask(credentials, s)
  val found = creds.exists {
    case dc: DirectCredentials if dc.userName == "doggie" ⇒ true
    case _ ⇒ false
  }
  assert(found, "Expected to find credentials for user 'doggie' defined in override-settings.xml")
}
