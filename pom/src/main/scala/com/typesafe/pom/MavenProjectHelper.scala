package com.typesafe.pom

import java.io.File

import org.apache.maven.model.{Dependency => PomDependency, Model => PomModel, Plugin => PomPlugin, Repository => PomRepository}
import scala.collection.JavaConverters._

object MavenProjectHelper {
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

  // TODO - Can we  pick a better name and does this need scrubbed?
  def makeProjectName(pom: PomModel, overrideName: Option[String]): String = {
    val pomName = Option(pom.getProperties.get("sbt.project.name").asInstanceOf[String])
    val directoryName = pom.getPomFile.getParentFile.getName
    overrideName.getOrElse(pomName.getOrElse(directoryName))
  }

  def makeProjectTree(pomFile: File, profiles: Seq[String], userProps: Map[String, String]): ProjectTree = {
    val pom = loadEffectivePom(pomFile, profiles = profiles, userProps = userProps)
    val children = getChildProjectPoms(pom, pomFile) map (makeProjectTree(_, profiles, userProps))
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
      childPom = new File(pomFile.getParentFile, s"$childDirName/pom.xml")
      if childPom.exists
    } yield childPom
}
