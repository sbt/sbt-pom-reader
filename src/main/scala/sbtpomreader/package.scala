import java.io.File

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.supplier.RepositorySystemSupplier

/** Helper methods for dealing with starting up Aether. */
package object sbtpomreader {
  def newRepositorySystemImpl: RepositorySystem =
    this.synchronized[RepositorySystem](new RepositorySystemSupplier().get())

  def newSessionImpl(system: RepositorySystem, localRepoDir: File) = {
    val session = MavenRepositorySystemUtils.newSession()
    val localRepo = new LocalRepository(localRepoDir.getAbsolutePath)
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session
  }

  def defaultLocalRepo: java.io.File = {
    import sbt.*
    file(sys.props("user.home")) / ".m2" / "repository"
  }

  def loadEffectivePom(
      pom: File,
      localRepo: File = defaultLocalRepo,
      profiles: Seq[String],
      userProps: Map[String, String],
      additionalRepositories: Seq[org.eclipse.aether.repository.RemoteRepository] = Seq.empty
  ) =
    MavenPomResolver(localRepo).loadEffectivePom(pom, additionalRepositories, profiles, userProps)
}
