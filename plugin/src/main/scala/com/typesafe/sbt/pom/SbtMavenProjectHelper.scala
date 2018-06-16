package com.typesafe.sbt.pom

import sbt._

/** This object knows how to load maven reactor projects and turn them into sbt projects. */
object SbtMavenProjectHelper {
  import com.typesafe.pom._, MavenProjectHelper._
  import SbtMavenHelper.useMavenPom

  def makeReactorProject(baseDir: File, overrideRootProjectName:Option[String] = None,
                         profiles: Seq[String], userProps: Map[String, String]): Seq[Project] = {
    // First create a tree of how things aggregate.
    val tree = makeProjectTree(baseDir / "pom.xml", profiles, userProps)
    // Next flatten the list of all projects.
    val projects = allProjectsInTree(tree)
    // Create a mapping of all dependencies between projects.
    val depMap = makeDependencyMap(projects)
    // Helper to look up dependencies in the presence of absences.
    def getDepsFor(project: ProjectTree): Seq[ProjectTree] =
      depMap.getOrElse(project, Nil)
    // Now, sort projects in an order that we can create them.
    val sorted: Seq[ProjectTree] =
      Dag.topologicalSort(projects) { project =>
        val aggregates = project match {
          case AggregateProject(_,_,children) => children
          case _ => Nil
        }
        val deps = getDepsFor(project)
        aggregates ++ deps
      }
    def makeProjects(toMake: Seq[ProjectTree], made: Map[ProjectTree, Project] = Map.empty): Seq[Project] =
      toMake match {
        case current :: rest =>
          // Make a project, and add it to the stack
          val depProjects: Seq[Project] =
            for {
              dep <- getDepsFor(current)
              depProject <- made.get(dep)
            } yield depProject
          val aggregates: Seq[Project] =
            current match {
              case AggregateProject(_,_, children) =>
                for {
                  child <- children
                  depProject <- made.get(child)
                } yield depProject
              case _ => Nil
            }
          // TODO - Configure debugging output....
          val currentProject = (
              Project(makeProjectName(current.model,overrideRootProjectName),current.dir)
              // First pull in settings from pom
              settings(useMavenPom:_*)
              // Now update depends on relationships
              dependsOn(depProjects.map(x =>x: ClasspathDep[ProjectReference]):_*)
              // Now fix aggregate relationships
              aggregate(aggregates.map(x => x:ProjectReference):_*)
              // Now remove any inter-project dependencies we pulled in from the maven pom.
              // TODO - Maybe we can fix the useMavenPom settings so we don't need to
              // post-filter artifacts?
              settings(
                Keys.libraryDependencies := {
                  val depIds = getDepsFor(current).map(_.id).toSet
                  Keys.libraryDependencies.value.filterNot { dep =>
                    val id = makeId(dep.organization, dep.name, dep.revision)
                    depIds contains id
                  }
                }
              )

          )
          makeProjects(rest, made + (current -> currentProject))
        case Nil => made.values.toSeq
      }
    makeProjects(sorted)
  }
}
