package com.typesafe.pom

import java.io.File
import java.util.Locale

import org.apache.maven.model.Model
import org.apache.maven.model.building.{DefaultModelBuilderFactory, DefaultModelBuildingRequest, ModelBuildingException, ModelBuildingRequest}
import org.apache.maven.model.resolution.ModelResolver
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.repository.RemoteRepository

import scala.collection.JavaConverters._

object MavenPomResolver {
  val system = newRepositorySystemImpl
  require(system != null, "Repository system failed to initialize")
  def apply(localRepo: File) = new MavenPomResolver(system, localRepo)
}


class MavenPomResolver(system: RepositorySystem, localRepo: File) {
   val session = newSessionImpl(system, localRepo)
   
   private val modelBuilder = (new DefaultModelBuilderFactory).newInstance
   
   private val defaultRepositories: Seq[RemoteRepository] =
     Seq(
       new RemoteRepository.Builder("central", "default", "http://repo.maven.apache.org/maven2").build()
     )

   // TODO - Add repositories from the pom...
   val modelResolver: ModelResolver = {
     new MavenModelResolver(
       session,
       system,
       repositories = defaultRepositories
     )
   }
   
   def loadEffectivePom(pomFile: File, repositories: Seq[RemoteRepository],
       activeProfiles: Seq[String], userPropsMap: Map[String, String]): Model =
     try {
       val userProperties = new java.util.Properties()
       userProperties.putAll(userPropsMap.asJava)
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
