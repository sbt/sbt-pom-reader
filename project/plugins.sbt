resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

// addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")

// addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")


// Until bintray-sbt 0.3.0 is published...
lazy val root = (project in file(".")).dependsOn(bintray)

lazy val bintray = uri("git://github.com/softprops/bintray-sbt.git#d8244dd")

libraryDependencies <+= (sbtVersion) { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

