import com.typesafe.sbt.pom.SbtPomKeys.settingsLocation

useMavenPom

settingsLocation := baseDirectory.value / "non-existent-file.xml"

TaskKey[Unit]("check-settings") <<= (state, baseDirectory) map { (s, basedir) =>
  val extracted = Project extract s
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
  testSetting(scalaVersion, "2.10.2")
  testSetting(organization, "com.jsuereth.junk")
  val expectedFile = basedir / "non-existent-file.xml"
  testSetting(settingsLocation, expectedFile)
  assert(!expectedFile.exists(), "File can't exist if I'm to test handling non-existence. Please delete " + expectedFile)
}
