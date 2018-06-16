
TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  val deps = extracted.get(libraryDependencies)

  assert(deps.exists(_.name.contains("bijection-core")), "Expected to find dependency `bijection-core`")
  assert(!deps.exists(_.name.contains("scalacheck")), "Should not have found dependency on `scala-check`")
  assert(!deps.exists(_.name.contains("specs")), "Should not have found dependency on `specs`")
}
