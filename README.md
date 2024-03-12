# sbt pom reader plugin

[![Build Status](https://github.com/sbt/sbt-pom-reader/actions/workflows/ci.yml/badge.svg)](https://github.com/sbt/sbt-pom-reader/actions)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/sbt/sbt-pom-reader)](https://github.com/sbt/sbt-pom-reader/tags)

This project aims to read Maven `pom.xml` files and configure a basic sbt project appropriately.  We have limited aims:

1. The intent is not to parse/use Maven plugins. The purpose is to translate project structure, dependencies, and artifact resolution
2. Failure to read a `pom.xml` file causes the Maven build to crash
3. Unexpected Maven-y things will simply fail to translate
4. Majority of simple Maven projects will be usable directly in sbt from the Maven `pom.xml`

That said:

1. Parent pom resolution and inheritance should work
2. Multi-module builds should work, assuming each module meets the above caveats


# Usage

You want you project directory to look like the following:

```
<my-maven-project>/
  pom.xml                  <- Your maven build.
  project/
     build.properties      <- the sbt version specification
     build.scala           <- the sbt build definition
     plugins.sbt           <- the sbt plugin configuration

  ..                       <- Whatever files are normally in your maven project.

```

Each of the files should have the following contents.

`project/build.properties`:

    sbt.version=1.9.9

`project/build.scala`:

    import sbt._
    object MyBuild extends sbtpomreader.PomBuild

`project/plugins.sbt`:

     addSbtPlugin("com.github.sbt" % "sbt-pom-reader" % "x.y.z")

NB: for sbt 0.13.x please use the plugin version prior to 2.5.0, as it is not supported anymore

`project/plugins.sbt`:

     addSbtPlugin("com.github.sbt" % "sbt-pom-reader" % "2.4.0")

## Configuring projects

If the pom-reader plugin doesn't have a 100% mapping from your maven build into sbt (i.e. there is some kind 
of configuration you wish to perform, then you will need to add specific configuration items.  Since the
plugin is automatically generating the sub-projects based on your maven configuration, you'll have to
indirectly reference them in sbt settings, like so:

`build.sbt`:
```
// This is a heuristic, assuming we're running sbt in the same directory as the build.
val buildLocation = (file(".").getAbsoluteFile.getParentFile)

// Here we define a reference to a subproject.  The string "subproject" refers to the artifact id of
// the subproject.
val subproject = ProjectRef(buildLoc, "subproject")

// Disable all scalac arguments when running the REPL.
scalacOptions in subproject in Compile in console := Seq.empty
```

# Contributing

Please feel free to contribute example/test maven projects you'd like to be able to load in sbt.  


# Licensing

Apache 2 software license.  See [LICENSE](LICENSE).


