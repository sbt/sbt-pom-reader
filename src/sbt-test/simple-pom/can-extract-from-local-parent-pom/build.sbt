
TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  def testSetting[T](key: SettingKey[T], expected: T): Unit = {
    val found = extracted get key
    assert(expected == found, "Failed to extract setting: " + key + ", expected: " + expected + ", found: " + found)
  }
  testSetting(name, "test-project")
  testSetting(version, "1.0-SNAPSHOT")
  testSetting(scalaVersion, "2.10.2")
  testSetting(organization, "com.jsuereth.junk")
  // TODO - test scalacOptions
  // TODO - test library dependencies.
}
