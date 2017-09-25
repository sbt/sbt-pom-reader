
settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  val rez = extracted.get(resolvers)
  // Should only pick up one of the two repositories due to profile activation.
  assert(rez.exists(_.name == "sbt-bintray"))
  assert(!rez.exists(_.name == "one-sad-repo"))
}
