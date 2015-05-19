# sbt pom reader plugin

[![Join the chat at https://gitter.im/sbt/sbt-pom-reader](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sbt/sbt-pom-reader?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/sbt/sbt-pom-reader.svg?branch=master)](https://travis-ci.org/sbt/sbt-pom-reader)

This project aims to read maven pom files and configure sbt appropriately.  We have limited aims:

1. We do not plan to parse/use maven plugins.  Only the ones which users submit handling code for
2. Failure to read a pom file causes the maven build to crash, sorry
3. Unexpected maven-y things will simply fail to translate.
4. Majority of simple maven projects will be usable directly in sbt from the maven pom.
5. Parent pom resolution and inheritance should work.
6. Multi-module builds should work assuming each project meets an above caveat (at least, once we implement support).


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

    sbt.version=0.13.8

`project/build.scala`:

    import sbt._
    object MyBuild extends com.typesafe.sbt.pom.PomBuild

`project/plugins.sbt`:

     addSbtPlugin("com.typesafe.sbt" % "sbt-pom-reader" % "1.0.1")
     

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


