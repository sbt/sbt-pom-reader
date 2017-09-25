enablePlugins(PomReaderPlugin)

TaskKey[Unit]("checkSettings") := {
  val extracted = Project extract state.value
  def testSetting[T](key: SettingKey[T], expected: T): Unit = {
    val found = extracted get key
    assert(expected == found, "Failed to extract setting: " + key + ", expected: " + expected + ", found: " + found)
  }
  def testSettingContains[T](key: SettingKey[Seq[T]], expected: T): Unit = {
    val values = extracted get key
    val found = values exists (_ == expected)
    assert(found, "Failed to extract setting: " + key + ", expected to find: " + expected + ", found: " + values)
  }
  // TODO - test licensing is apache's
}
