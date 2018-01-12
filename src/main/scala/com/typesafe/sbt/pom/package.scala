package com.typesafe.sbt

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.transport.wagon.{WagonProvider, WagonTransporterFactory}
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.DefaultRepositorySystemSession
import java.io.File

/** Helper methods for dealing with starting up Aether. */
package object pom {
  def newRepositorySystemImpl: RepositorySystem = {
    val locator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(classOf[TransporterFactory], classOf[WagonTransporterFactory])
    locator.setServices(classOf[WagonProvider], new HackedWagonProvider)
    locator.getService(classOf[RepositorySystem])
  }
  def newSessionImpl(system: RepositorySystem, localRepoDir: File)  = {
    val session = new DefaultRepositorySystemSession
    val localRepo = new LocalRepository(localRepoDir.getAbsolutePath)
    session setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session
  }
  
  def defaultLocalRepo: java.io.File = {
    import sbt._
    (file(sys.props("user.home")) / ".m2" / "repository")
  }
  
  def loadEffectivePom(pom: File, localRepo: File = defaultLocalRepo, profiles: Seq[String], userProps: Map[String, String]) =
    MvnPomResolver(localRepo).loadEffectivePom(pom, Seq.empty, profiles, userProps)
}
