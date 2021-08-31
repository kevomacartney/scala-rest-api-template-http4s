package org.example.com

import cats.effect._
import com.codahale.metrics.MetricRegistry
import cats.syntax.all._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.example.com.config.ApplicationConfig
import org.example.com.endpoints.{ApiEndpoint, Endpoints}
import org.example.com.ops.OpsServer
import org.example.com.services.ApiService
import org.http4s.blaze.server.BlazeServerBuilder
import org.slf4j.LoggerFactory
import java.time.Clock
import scala.concurrent.ExecutionContext.global

object Wiring {
  def initialise(
      appConfig: ApplicationConfig
  )(implicit cs: ContextShift[IO]): Resource[IO, (Server, Server)] = {
    val logger = LoggerFactory.getLogger("Example-Rest-API")
    logger.info("Starting app")

    implicit val metricsRegistry: MetricRegistry = new MetricRegistry
    implicit val clock: Clock                    = Clock.systemUTC()

    for {
      itemRepository <- ItemRepository()
      ops            <- new OpsServer().create(appConfig.opsServerConfig)
      api            <- createServer(itemRepository, appConfig)
    } yield (ops, api)

  }

  private def createServer(repository: ItemRepository, appConfig: ApplicationConfig)(
      implicit cs: ContextShift[IO],
      metricRegistry: MetricRegistry
  ): Resource[IO, Server] = {
    val httpService = new ApiService(repository)
    val server      = Router("/" -> httpService.helloWorldService).orNotFound

    BlazeServerBuilder[IO](global)
      .bindHttp(appConfig.restConfig.port, "localhost")
      .withHttpApp(server)
      .resource
  }
}
