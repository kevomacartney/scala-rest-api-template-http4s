package org.example.com

import cats.effect._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.example.com.config.ApplicationConfig
import pureconfig.ConfigSource
import collection.JavaConverters._
import scala.io.{BufferedSource, Source}

object Application extends LazyLogging {

  def run(env: String)(implicit cs: ContextShift[IO]): IO[ExitCode] = {
    val resultResource = for {
      secrets   <- loadSecrets
      appConfig <- loadConfig(secrets)
      wiring    <- Wiring.initialise(appConfig)
    } yield wiring

    resultResource
      .use { tuple => // can't figure out how to open it up here
        val (service, opServer) = tuple
        logger.info(s"Server started and listening on ${service.address}")
        logger.info(s"Ops Server started and listening on ${opServer.address}")

        IO.never
      }
      .handleErrorWith { ex =>
        logger.error("Server terminated due to error", ex)
        IO.raiseError(ex)
      }
  }

  def loadConfig(secrets: Map[String, String]): Resource[IO, ApplicationConfig] = { // required
    import pureconfig.generic.auto._

    val defaultConfigResource = Resource.eval(IO(ConfigFactory.parseResources("default.conf")))
    val secretsConfigResource = Resource.eval(IO(ConfigFactory.parseMap(secrets.asJava)))

    for {
      defaultConfig <- defaultConfigResource
      secretsConfig <- secretsConfigResource
      mergedConfig  = ConfigFactory.load().withFallback(defaultConfig).withFallback(secretsConfig).resolve()
      appConfig     = ConfigSource.fromConfig(mergedConfig).loadOrThrow[ApplicationConfig]
    } yield appConfig
  }

  def loadSecrets: Resource[IO, Map[String, String]] = {
    import io.circe.parser._
    val acquire            = IO(Source.fromFile(this.getClass.getResource("/secrets.json").getPath))
    val release            = (bS: BufferedSource) => IO(bS.close())
    val fileBufferResource = Resource.make(acquire)(release)

    for {
      buffer <- fileBufferResource
      str    = buffer.getLines().seq.mkString("\n")
      json   = decode[Map[String, String]](str).getOrElse(throw new RuntimeException("Invalid secrets file"))
    } yield json

  }
}
