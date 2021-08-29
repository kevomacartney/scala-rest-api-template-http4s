package org.example.com

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.{Request, Response}
import io.catbird.util.effect.futureToAsync
import org.example.com.config.ApplicationConfig
import org.example.com.endpoints.{ApiEndpoint, Endpoints}
import org.example.com.httpClient.MetricsStatsReceiver
import org.example.com.ops.OpsServer
import org.slf4j.LoggerFactory

import java.time.Clock

object Wiring {
  def initialise(
      appConfig: ApplicationConfig
  )(implicit cs: ContextShift[IO]): Resource[IO, (ListeningServer, ListeningServer)] = {
    val logger = LoggerFactory.getLogger("Example-Rest-API")
    logger.info("Starting app")

    implicit val metricsRegistry: MetricRegistry = new MetricRegistry
    implicit val clock: Clock                    = Clock.systemUTC()

    for {
      repository <- ItemRepository()
      opsServer  <- new OpsServer().create(appConfig.opsServerConfig)
      service    <- createServer(repository, appConfig)
    } yield (opsServer, service)
  }

  private def createServer(repository: ItemRepository, appConfig: ApplicationConfig)(
      implicit cs: ContextShift[IO],
      metricRegistry: MetricRegistry
  ): Resource[IO, ListeningServer] = {
    val services = initialiseServices(repository, metricRegistry)

    val acquire: IO[ListeningServer] = IO {
      Http.server
        .withLabel("example-api")
        .withStatsReceiver(MetricsStatsReceiver(metricRegistry))
        .serve(s":${appConfig.restConfig.port}", services)
    }
    val release: ListeningServer => IO[Unit] = (server: ListeningServer) => futureToAsync[IO, Unit](server.close())

    Resource.make(acquire)(release)
  }

  private def initialiseServices(repository: ItemRepository, metricRegistry: MetricRegistry)(
      implicit cs: ContextShift[IO]
  ): Service[Request, Response] = {
    val apiEndpoint = new ApiEndpoint(repository, metricRegistry)
    Endpoints.createServices(apiEndpoint)
  }
}
