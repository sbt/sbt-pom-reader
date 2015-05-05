package com.typesafe.sbt

import org.apache.maven.repository.internal.MavenServiceLocator
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.connector.wagon.{
  WagonProvider, 
  WagonRepositoryConnectorFactory
}
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory
import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.RepositorySystemSession
import java.io.File

/** Helper methods for dealing with starting up Aether. */
package object pom {
  def newRepositorySystemImpl: RepositorySystem = {
    val locator = new MavenServiceLocator
    locator.addService(classOf[RepositoryConnectorFactory], classOf[WagonRepositoryConnectorFactory])
    locator.setServices(classOf[WagonProvider], new HackedWagonProvider)
    locator.getService(classOf[RepositorySystem])
  }
  def newSessionImpl(system: RepositorySystem, localRepoDir: File): RepositorySystemSession  = {
    val session = new MavenRepositorySystemSession
    val localRepo = new LocalRepository(localRepoDir.getAbsolutePath)
    session setLocalRepositoryManager (system newLocalRepositoryManager localRepo)
    session
  }
  
  def defaultLocalRepo: java.io.File = {
    import sbt._
    (file(sys.props("user.home")) / ".m2" / "repository")
  }
  
  def loadEffectivePom(pom: File, localRepo: File = defaultLocalRepo, profiles: Seq[String], userProps: Map[String, String]) =
    MvnPomResolver(localRepo).loadEffectivePom(pom, Seq.empty, profiles, userProps)
}
