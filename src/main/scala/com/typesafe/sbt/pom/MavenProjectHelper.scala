package com.typesafe.sbt.pom

import sbt._
import Keys._
import org.apache.maven.model.{
  Model => PomModel,
  Plugin => PomPlugin,
  Dependency => PomDependency,
  Repository => PomRepository
}
import Project.Initialize
import SbtPomKeys._
import collection.JavaConverters._

/** This object knows how to load maven reactor projects and turn them into sbt projects. */
object MavenProjectHelper {
  import MavenHelper.useMavenPom
  
  def makeId(group: String, art: String, version: String): String =
    group+":"+art+":"+version
  
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
    override def toString = "Project("+id+")"
    override def equals(that: Any): Boolean = 
      that match {
        case other: ProjectTree => id == other.id
        case _ => false
      }
  }
  case class AggregateProject(model: PomModel, dir: File, children: Seq[ProjectTree]) extends ProjectTree
  case class SimpleProject(model: PomModel, dir: File) extends ProjectTree
  
  def makeReactorProject(baseDir: File): Seq[Project] = {
    // First create a tree of how things aggregate.
    val tree = makeProjectTree(baseDir / "pom.xml")
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
              Project(makeProjectName(current.model),current.dir)
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
                Keys.libraryDependencies <<= Keys.libraryDependencies apply { deps =>
                  val depIds = getDepsFor(current).map(_.id).toSet
                  deps filterNot { dep =>
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
  
  // TODO - Can we  pick a better name and does this need scrubbed?
  def makeProjectName(pom: PomModel): String = {
    val directoryName = pom.getPomFile.getParentFile.getName
    directoryName
  }
    
  def makeProjectTree(pomFile: File): ProjectTree = {
    val pom = loadEffectivePom(pomFile)
    val children = getChildProjectPoms(pom, pomFile) map makeProjectTree
    if(children.isEmpty) SimpleProject(pom, pomFile.getParentFile)
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
      for(project <- projects) yield {
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