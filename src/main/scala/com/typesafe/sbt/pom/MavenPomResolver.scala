package com.typesafe.sbt.pom


import org.sonatype.aether.repository.RemoteRepository
import org.apache.maven.model.Model
import org.sonatype.aether.RepositorySystem
import java.io.File
import org.apache.maven.model.building.{
  DefaultModelBuildingRequest, 
  ModelBuildingRequest,
  ModelBuildingException,
  DefaultModelBuilderFactory
}
import collection.JavaConverters._
import java.util.Locale
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.artifact.resolver.DefaultArtifactResolver

object MvnPomResolver {
  val system = newRepositorySystemImpl
  def apply(localRepo: File) = new MvnPomResolver(system, localRepo)
}


class MvnPomResolver(system: RepositorySystem, localRepo: File) {
   val session = newSessionImpl(system, localRepo)
   
   private val modelBuilder = (new DefaultModelBuilderFactory).newInstance
   
   private val defaultRepositories: Seq[RemoteRepository] =
     Seq(
       new RemoteRepository( "central", "default", " http://repo.maven.apache.org/maven2" )    
     )
   
   // TODO - Add repositories from the pom...
   val modelResolver: ModelResolver = {
     new MyModelResolver(
       session,
       system,
       repositories = defaultRepositories
     )
   }
   
   def loadEffectivePom(pomFile: File, repositories: Seq[RemoteRepository],
       activeProfiles: Seq[String]): Model =
     try {
       val request = new DefaultModelBuildingRequest
       request setLocationTracking true
       request setProcessPlugins false
       request setPomFile pomFile
       request setValidationLevel ModelBuildingRequest.VALIDATION_LEVEL_STRICT
       // TODO - Pass as arguments?
       request setSystemProperties systemProperties
       request setUserProperties userProperties
       request setActiveProfileIds activeProfiles.asJava
       // TODO - Model resolver?
       request setModelResolver modelResolver

       (modelBuilder build request).getEffectiveModel
     } catch {
       case e: ModelBuildingException =>
         // TODO - Wrap in better exception...
         throw e
     }
   // TODO - Allow these to be specified...
   val userProperties = new java.util.Properties
   
   lazy val systemProperties = {
     val props = new java.util.Properties
     props putAll envProperties.asJava
     props putAll System.getProperties
     // TODO - Add more?
     props
   }
   
   lazy val envProperties: Map[String, String] = {
     val caseInsenstive = false // TODO - is windows?
     System.getenv.entrySet.asScala.map { entry =>
       val key = "env." + (
           if(caseInsenstive) entry.getKey.toUpperCase(Locale.ENGLISH) 
           else entry.getKey)
       key -> entry.getValue
     } {collection.breakOut}
   }
}
