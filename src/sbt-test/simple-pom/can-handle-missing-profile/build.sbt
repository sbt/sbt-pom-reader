settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("check-settings") := {
  val extracted = Project extract state.value
  val r = extracted get resolvers
  assert(r.nonEmpty, "Expected at least one resolver")
}
