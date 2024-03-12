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
  testSetting(name, "test-project")
  testSetting(version, "1.0-SNAPSHOT")
  testSetting(scalaVersion, "2.13.13")
  testSetting(organization, "com.jsuereth.junk")
  testSettingContains(resolvers, "DUMMY-REPO" at "http://my-dum-repo.org")
  // TODO - test scalacOptions
  // TODO - test library dependencies.
}
