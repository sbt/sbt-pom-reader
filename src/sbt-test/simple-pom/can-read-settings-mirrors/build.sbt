
settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("checkMirrors") := {
  val settings = effectiveSettings.value
  assert(settings.isDefined, "Expected effective settings to be defined")
  val mirrors = settings.get.getMirrors
  assert(mirrors.size == 1, s"Expected 1 mirror, got ${mirrors.size}")
  val mirror = mirrors.get(0)
  assert(mirror.getId == "my-mirror", s"Expected mirror id 'my-mirror', got '${mirror.getId}'")
  assert(mirror.getMirrorOf == "central", s"Expected mirrorOf 'central', got '${mirror.getMirrorOf}'")
  assert(mirror.getUrl == "https://mirror.example.com/maven2", s"Expected mirror url 'https://mirror.example.com/maven2', got '${mirror.getUrl}'")
}
