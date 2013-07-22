# sbt pom reader plugin

This project aims to read maven pom files and configure sbt appropriately.  We have limited aims:

1. We do not plan to parse/use maven plugins.  Only the ones which users submit handling code for
2. Failure to read a pom file causes the maven build to crash, sorry
3. Unexpected maven-y things will simply fail to translate.
4. Majority of simple maven projects will be usable directly in sbt from the maven pom.
5. Parent pom resolution and inheritance should work.
6. Multi-module builds should work assuming each project meets an above caveat (at least, once we implement support).


The only config you should require in your `build.sbt` for a simple maven project is:

    useMavenPom

The only thing you should require for a reactor maven project is in your `project/builds.scala`:

    import sbt._
    object MyBuild extends com.typesafe.sbt.pom.PomBuild


Please feel free to contribute example/test maven projects you'd like to be able to load in sbt.


# Licensing

Apache 2 software license.  Links and stuff to follow.


