package sbtpomreader

import scala.collection.JavaConverters._

import org.apache.maven.model.{ Dependency, Parent, Repository }
import org.apache.maven.model.building.{ FileModelSource, ModelSource2 }
import org.apache.maven.model.resolution.{ ModelResolver, UnresolvableModelException }
import org.apache.maven.repository.internal.ArtifactDescriptorUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.{ ArtifactRequest, ArtifactResolutionException }

/**
 * We implement this because maven hides theirs. RUN BUT YOU CAN'T HIDE, LITTLE MAVEN.
 */
class MavenModelResolver(
    session: RepositorySystemSession,
    system: RepositorySystem,
    context: String = "",
    repositories: Seq[RemoteRepository] = Nil
) extends ModelResolver {

  private[this] var _repositories: Seq[RemoteRepository] = repositories

  override def resolveModel(groupId: String, artifactId: String, version: String): ModelSource2 = {
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

  override def resolveModel(dependency: Dependency): ModelSource2 =
    resolveModel(dependency.getGroupId, dependency.getArtifactId, dependency.getVersion)

  override def resolveModel(parent: Parent): ModelSource2 =
    resolveModel(parent.getGroupId, parent.getArtifactId, parent.getVersion)

  override def newCopy =
    new MavenModelResolver(session, system, context, _repositories)

  override def addRepository(repository: Repository): Unit = addRepository(repository, false)

  def resetRepositories(): Unit = _repositories = repositories

  def addRepository(repository: Repository, replace: Boolean): Unit = {
    val exists = _repositories.exists(_.getId == repository.getId)
    if (!exists || replace) {
      // TODO - Should we use the remote repo manager?
      val newRemote: RemoteRepository = ArtifactDescriptorUtils.toRemoteRepository(repository)
      _repositories = _repositories :+ newRemote
    }
  }
}
