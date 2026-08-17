# Changelog

## v2.5.0

### Project Management

* Rework build definition
* Update Maven dependencies
* Project doesn't refer to unsecure HTTP repository URLs anymore

## v2.4.0 + v2.3.0

### Project Management

* Project artifact is now published under `com.github.sbt` org name and `sbtpomreader` package name.
* CI moved to Github Actions

## v2.2.0

### Enhancements

* Handle Maven classifier corner cases, that fixes a few related bugs

### Project Management

* Dependencies updates
* Fixes for the tests

## v2.1.0

### New Features

* Support for sbt 1.0.2 (@blast-hardcheese)

### Project Management

* General code and dependency cleanup (@blast-hardcheese, @xuwei-k)
* Fixed scripted tests to work with sbt 1.0.2 (@metasim)

## v2.0.0

### Enhancements

* Converted processing of `settings.xml` to use official Maven library enabling proper handling of profiles.
* Ability to include resolvers (repositories) defined in `settings.xml`.
* Ability to override location of `settings.xml` file via `settingsLocation` key.
* Converted to `AutoPlugin`

### Bug Fixes

* Fixed scripted commands with incorrect subproject references
* Provided default for `mavenUserProperties`.

### Project Management

* Added [Travis-CI build](https://travis-ci.org/sbt/sbt-pom-reader) support.
* Added [Bintray](https://bintray.com/sbt/sbt-plugin-releases/sbt-pom-reader) support.
* Additional `scripted` tests around `settings.xml` file handling and repository credentials processing.

## When the Earth was still cooling

Please, refer to git history.
