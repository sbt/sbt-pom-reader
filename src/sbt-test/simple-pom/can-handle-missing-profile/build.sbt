settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("check-settings") <<= state map { s =>
  val extracted = Project extract s
  val r = extracted get resolvers
  assert(r.nonEmpty, "Expected at least one resolver")
}
