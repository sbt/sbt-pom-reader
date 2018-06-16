
TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  def testSetting[T](key: SettingKey[T], expected: T): Unit = {
    val found = extracted get key
    assert(expected == found, "Failed to extract setting: " + key + ", expected: " + expected + ", found: " + found)
  }
  testSetting(scalaVersion, "2.10.1-TEST")
}
