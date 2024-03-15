package sbtpomreader

import sbt._

import scala.collection.JavaConverters._

import org.apache.maven.model.{ Model => PomModel }

/** This object knows how to load maven reactor projects and turn them into sbt projects. */
object MavenProjectHelper {
  import MavenHelper.useMavenPom

  def makeId(group: String, art: String, version: String): String =
    group + ":" + art + ":" + version

  /** A way of treating projects as trees. */
  sealed trait ProjectTree {
    def model: PomModel
    def dir: File

    // TODO - Keep version?
    lazy val id: String =
      makeId(model.getGroupId, model.getArtifactId, model.getVersion)

    def name: String =
      model.getArtifactId

    override def hashCode = id.hashCode
    override def toString = "Project(" + id + ")"
    override def equals(that: Any): Boolean =
      that match {
        case other: ProjectTree => id == other.id
        case _                  => false
      }
  }
  case class AggregateProject(model: PomModel, dir: File, children: Seq[ProjectTree]) extends ProjectTree
  case class SimpleProject(model: PomModel, dir: File) extends ProjectTree

  def makeReactorProject(
      baseDir: File,
      overrideRootProjectName: Option[String] = None,
      profiles: Seq[String],
      userProps: Map[String, String]
  ): Seq[Project] = {
    // First create a tree of how things aggregate.
    val tree = makeProjectTree(baseDir / "pom.xml", profiles, userProps)
    // Next flatten the list of all projects.
    val projects = allProjectsInTree(tree)
    // Create a mapping of all dependencies between projects.
    val depMap: Map[ProjectTree, Seq[ProjectTree]] = makeDependencyMap(projects)
    // Helper to look up dependencies in the presence of absences.
    def getDepsFor(project: ProjectTree): Seq[ProjectTree] =
      depMap.getOrElse(project, Nil)
    // Now, sort projects in an order that we can create them.
    val sorted: Seq[ProjectTree] =
      Dag.topologicalSort(projects) { project =>
        val aggregates = project match {
          case AggregateProject(_, _, children) => children
          case _                                => Nil
        }
        val deps = getDepsFor(project)
        aggregates ++ deps
      }
    def makeProjects(toMake: Seq[ProjectTree], made: Map[ProjectTree, (Project, ModuleID)] = Map.empty): Seq[Project] =
      toMake match {
        case current :: rest =>
          // Make a project, and add it to the stack
          val depProjects: Seq[(Project, ModuleID)] =
            for {
              dep <- getDepsFor(current)
              depProject <- made.get(dep)
            } yield depProject
          val aggregates: Seq[Project] =
            current match {
              case AggregateProject(_, _, children) =>
                for {
                  child <- children
                  depProject <- made.get(child)
                } yield depProject._1
              case _ => Nil
            }
          // Prepare data:
          // list of projects and their moduleIds with correct configurations,
          // according to current node in POM we are processing now
          val modulesMap: Map[String, Seq[ModuleID]] =
            MavenHelper
              .getModuleDependencies(current.model)
              .groupBy { m =>
                makeId(m.organization, m.name, m.revision)
              }
          val projectsMap: Map[String, Seq[(Project, ModuleID)]] = depProjects.groupBy { case (p, m) =>
            makeId(m.organization, m.name, m.revision)
          }
          val projectsWithModules: Seq[(Project, ModuleID)] = modulesMap.keySet
            .map { id =>
              val mwc: Seq[ModuleID] = modulesMap.get(id).getOrElse(Seq.empty)
              val projects: Seq[Project] = projectsMap.get(id).getOrElse(Seq.empty).map(_._1)
              projects.zip(mwc)
            }
            .flatten
            .toSeq

          // TODO - Configure debugging output....
          val currentProject: Project = (
            Project(makeProjectName(current.model, overrideRootProjectName), current.dir)
            // First pull in settings from pom
              settings (useMavenPom: _*)
              // Now update depends on relationships with actual configurations
              dependsOn (projectsWithModules.map { case (p, m) => new ClasspathDependency(p, m.configurations) }: _*)
              // Now fix aggregate relationships
              aggregate (aggregates.map(x => x: ProjectReference): _*)
              // Now remove any inter-project dependencies we pulled in from the maven pom.
              // TODO - Maybe we can fix the useMavenPom settings so we don't need to
              // post-filter artifacts?
              settings (
                Keys.libraryDependencies := {
                  val depIds = getDepsFor(current).map(_.id).toSet
                  Keys.libraryDependencies.value.filterNot { dep =>
                    val id = makeId(dep.organization, dep.name, dep.revision)
                    depIds contains id
                  }
                }
              )
          )
          val currentModule = ModuleID(current.model.getGroupId, current.model.getArtifactId, current.model.getVersion)
          makeProjects(rest, made + (current -> (currentProject, currentModule)))
        case Nil => made.values.map(_._1).toSeq
      }
    makeProjects(sorted)
  }

  // TODO - Can we  pick a better name and does this need scrubbed?
  def makeProjectName(pom: PomModel, overrideName: Option[String]): String = {
    val pomName = Option(pom.getProperties.get("sbt.project.name").asInstanceOf[String])
    val directoryName = pom.getPomFile.getParentFile.getName
    overrideName.getOrElse(pomName.getOrElse(directoryName))
  }

  def makeProjectTree(pomFile: File, profiles: Seq[String], userProps: Map[String, String]): ProjectTree = {
    val pom = loadEffectivePom(pomFile, profiles = profiles, userProps = userProps)
    val children = getChildProjectPoms(pom, pomFile) map (makeProjectTree(_, profiles, userProps))
    if (children.isEmpty) SimpleProject(pom, pomFile.getParentFile)
    else AggregateProject(pom, pomFile.getParentFile, children)
  }

  // An unsorted walk of the tree
  def allProjectsInTree(tree: ProjectTree): Seq[ProjectTree] =
    tree match {
      case x: SimpleProject => Seq(x)
      case agg: AggregateProject =>
        Seq(agg) ++ agg.children.flatMap(allProjectsInTree)
    }
  // Detects dependencies between projects
  def makeDependencyMap(projects: Seq[ProjectTree]): Map[ProjectTree, Seq[ProjectTree]] = {
    val findDeps =
      for (project <- projects) yield {
        val deps =
          for {
            dep <- Option(project.model.getDependencies).map(_.asScala).getOrElse(Nil)
            depId = makeId(dep.getGroupId, dep.getArtifactId, dep.getVersion)
            pdep <- projects
            if pdep.id == depId
          } yield pdep
        project -> deps
      }
    findDeps.toMap
  }

  def getChildProjectPoms(pom: PomModel, pomFile: File): Seq[File] =
    for {
      childDirName <- Option(pom.getModules) map (_.asScala) getOrElse Nil
      childPom = pomFile.getParentFile / childDirName / "pom.xml"
      if childPom.exists
    } yield childPom
}
