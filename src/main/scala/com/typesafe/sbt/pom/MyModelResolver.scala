package com.typesafe.sbt.pom


import java.io.File
import org.apache.maven.model.{Dependency, Parent, Repository}
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelSource
import org.apache.maven.model.resolution.InvalidRepositoryException
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.resolution.UnresolvableModelException
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.RequestTrace
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.impl.ArtifactResolver
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.artifact.DefaultArtifact
import collection.JavaConverters._
import org.apache.maven.repository.internal.ArtifactDescriptorUtils
import org.eclipse.aether.RepositorySystem
/**
 * We implement this because maven hides theirs.  RUN BUT YOU CAN'T HIDE, LITTLE MAVEN.
 */
class MyModelResolver(
  session: RepositorySystemSession,
  system: RepositorySystem,
  context: String = "",
  repositories: Seq[RemoteRepository] = Nil
) extends ModelResolver {

  private[this] var _repositories: Seq[RemoteRepository] = repositories
  
  override def resolveModel(parent: Parent): ModelSource =
    resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion())

  override def resolveModel(dependency: Dependency): ModelSource = {
    resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())
  }

  override def resolveModel(
    groupId: String,
    artifactId: String,
    version: String
  ): ModelSource = {
    val pomArtifact = 
      try {
        val tmp = new DefaultArtifact(groupId, artifactId, "", "pom", version)
        val request = new ArtifactRequest(tmp, _repositories.asJava, context)
        system.resolveArtifact(session, request).getArtifact
      } catch {
        case e: ArtifactResolutionException =>
           throw new UnresolvableModelException(e.getMessage, groupId, artifactId, version, e)
      }
    new FileModelSource(pomArtifact.getFile)
  }
  
  override def newCopy = 
    new MyModelResolver(session, system, context, _repositories)
  
  override def addRepository(repository: Repository, replace: Boolean): Unit = {
     val exists =
       _repositories.exists(_.getId == repository.getId)
     if(!exists || replace) {
       // TODO - Should we use the remote repo manager?
       val newRemote = ArtifactDescriptorUtils.toRemoteRepository(repository) 
       _repositories = _repositories.filterNot(_.getId == repository.getId)
       _repositories :+= newRemote
     }
   }

  override def addRepository(repository: Repository): Unit =
    addRepository(repository, false)
}
