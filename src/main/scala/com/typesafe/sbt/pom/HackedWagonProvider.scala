package com.typesafe.sbt.pom

import java.lang.reflect.Field
import java.security.AccessController
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.providers.file.FileWagon
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon
import org.sonatype.aether.connector.wagon.WagonProvider
import org.apache.maven.wagon.providers.http.LightweightHttpWagon


/** We hack our own wagon provider, since plexus isn't working for us right now. */
class HackedWagonProvider extends WagonProvider {

    override def lookup(roleHint: String): Wagon = roleHint match {
      case "http"  => setAuthenticator(new LightweightHttpWagon)
      case "https" => setAuthenticator(new LightweightHttpsWagon)
      case "file"  => new FileWagon
      case _       =>  sys.error("Wagon role hint not supported: " + roleHint);
    }

    // WTF - should we do something?
    override def release(wagon: Wagon): Unit = ()

    // SHRINKRES-68
    // Wagon does not correctly fill Authenticator field if Plexus is not used
    // we need to use reflection in order to get fix this behavior
    // http://dev.eclipse.org/mhonarc/lists/aether-users/msg00113.html
    private def setAuthenticator(wagon: LightweightHttpWagon ): LightweightHttpWagon =  {
      val authenticator =
      try {
        AccessController.doPrivileged(new PrivilegedExceptionAction[Field] {
          def run(): Field = {
            val field = classOf[LightweightHttpWagon].getDeclaredField("authenticator")
            field.setAccessible(true)
            field
          }
        });
      } catch {
        case ex: PrivilegedActionException =>
            throw new RuntimeException("Could not manually set authenticator to accessible on "
                + classOf[LightweightHttpWagon].getName, ex)
      }
      try authenticator.set(wagon, new LightweightHttpWagonAuthenticator)
      catch {
        case e: Exception =>
            throw new RuntimeException("Could not manually set authenticator on "
                + classOf[LightweightHttpWagon].getName, e)
        }

      // Borrowed from code readings online ->
      // Needed to ensure that we do not cache BASIC Auth values
      wagon.setPreemptiveAuthentication(true)
      wagon
    }
}