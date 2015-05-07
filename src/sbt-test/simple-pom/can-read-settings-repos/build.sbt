
settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("check-settings") <<= state map { s =>
  val extracted = Project extract s
  val rez = extracted.get(resolvers)
  // Should only pick up one of the two repositories due to profile activation.
  assert(rez.exists(_.name == "sbt-bintray"))
  assert(!rez.exists(_.name == "one-sad-repo"))
}
