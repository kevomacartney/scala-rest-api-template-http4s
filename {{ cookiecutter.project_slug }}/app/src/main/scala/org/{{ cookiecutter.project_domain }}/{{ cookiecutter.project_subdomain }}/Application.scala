package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}

import cats.effect._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config.ApplicationConfig
import pureconfig.ConfigSource

import scala.jdk.CollectionConverters._

object Application extends LazyLogging {
  def run(env: String, secrets: Map[String, String])(
      implicit meterRegistry: PrometheusMeterRegistry
  ): Resource[IO, Unit] = {
    val resultResource = for {
      appConfig <- loadConfig(secrets)
      wiring    <- Wiring.initialise(appConfig)
    } yield wiring

    resultResource
      .map { tuple => // tuple is (Server, Server)
        val (service, opServer) = tuple
        logger.info(s"Server started and listening on ${service.address}")
        logger.info(s"Ops Server started and listening on ${opServer.address}")
      }
  }

  private def loadConfig(secrets: Map[String, String]): Resource[IO, ApplicationConfig] = {
    import pureconfig.generic.auto._ // required for pureconfig to work

    val defaultConfigResource = Resource.eval(IO(ConfigFactory.parseResources("application.conf")))
    val secretsConfigResource = Resource.eval(IO(ConfigFactory.parseMap(secrets.asJava)))

    for {
      defaultConfig <- defaultConfigResource
      secretsConfig <- secretsConfigResource
      mergedConfig  = secretsConfig.withFallback(defaultConfig)
      appConfig     = ConfigSource.fromConfig(mergedConfig).loadOrThrow[ApplicationConfig]
    } yield appConfig
  }
}
