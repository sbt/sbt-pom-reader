enablePlugins(GitBranchPrompt)

enablePlugins(GitVersioning)

git.baseVersion := "2.1"

git.useGitDescribe := true

// Not sure why the following are necessary...
git.gitDescribedVersion := git.gitDescribedVersion((v) => v.map(_.drop(1))).value

git.gitTagToVersionNumber := { tag: String =>
  if(tag matches "v[0.9]+\\..*") Some(tag.drop(1))
  else None
}
