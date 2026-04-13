
settingsLocation := baseDirectory.value / "override-settings.xml"

TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  val rez = extracted.get(resolvers)
  // Should only pick up one of the two repositories due to profile activation.
  assert(rez.exists(_.name == "sbt-bintray"), "Expected sbt-bintray in resolvers, found: " + rez)
  assert(!rez.exists(_.name == "one-sad-repo"), "Unexpected one-sad-repo in resolvers")

  // Verify effective POM loaded successfully with settings repos wired in.
  val pom = extracted.get(effectivePom)
  assert(pom != null, "effectivePom should be loaded")
  assert(pom.getGroupId == "com.jsuereth.junk", "Expected groupId com.jsuereth.junk, found: " + pom.getGroupId)
}
