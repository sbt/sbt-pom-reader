# Release Process Outline

0. Write release notes
1. Confirm value of `git.baseVersion` in `version.sbt`
2. `git tag v<version> && git push`
3. `sbt scripted publish`
4. Confirm everything's OK; check Bintray package
5. `sbt bintrayRelease`
6. Update `git.baseVersion` to next release.
7. Update `README.md` to reference new binary version.
8. Commit and push.
9. Point Bintray package to release notes
10. Create a release entry in GitHub(?)

