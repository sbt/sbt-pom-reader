package com.typesafe.sbt.pom


import java.io.File
import org.apache.maven.model.Repository
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelSource
import org.apache.maven.model.resolution.InvalidRepositoryException
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.resolution.UnresolvableModelException
import org.sonatype.aether.RepositorySystemSession
import org.sonatype.aether.RequestTrace
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.impl.ArtifactResolver
import org.sonatype.aether.impl.RemoteRepositoryManager
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.ArtifactRequest
import org.sonatype.aether.resolution.ArtifactResolutionException
import org.sonatype.aether.util.artifact.DefaultArtifact
import collection.JavaConverters._
import org.apache.maven.repository.internal.ArtifactDescriptorUtils
import org.sonatype.aether.RepositorySystem
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
  
  override def addRepository(repository: Repository): Unit = {
     val exists =
       _repositories.exists(_.getId == repository.getId)
     if(!exists) {
       // TODO - Should we use the remote repo manager?
       val newRemote = ArtifactDescriptorUtils.toRemoteRepository(repository) 
       _repositories :+= newRemote
     }
   }
}