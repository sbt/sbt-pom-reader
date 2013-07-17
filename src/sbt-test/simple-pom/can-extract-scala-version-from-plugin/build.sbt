useMavenPom

TaskKey[Unit]("check-settings") <<= state map { s =>
  val extracted = Project extract s
  def testSetting[T](key: SettingKey[T], expected: T): Unit = {
    val found = extracted get key
    assert(expected == found, "Failed to extract setting: " + key + ", expected: " + expected + ", found: " + found)
  }
  testSetting(scalaVersion, "2.10.1-TEST")
}