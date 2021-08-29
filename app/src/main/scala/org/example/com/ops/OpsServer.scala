package org.example.com.ops

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.twitter.finagle.http._
import com.twitter.finagle._
import com.twitter.finagle.http.filter.{CommonLogFormatter, LoggingFilter}
import com.typesafe.scalalogging.LazyLogging
import io.catbird.util.effect.futureToAsync
import io.circe.Encoder
import io.finch._
import org.example.com.config.OpsServerConfig
import org.example.com.http._
import org.example.com.httpClient._
import com.twitter.logging.{Logger => TwitterLogger}

class OpsServer extends LazyLogging {
  def create(
      opsServerConfig: OpsServerConfig
  )(implicit metricRegistry: MetricRegistry, cs: ContextShift[IO]): Resource[IO, ListeningServer] = {
    val acquire = IO(
      Http.server
        .withLabel("ops-server")
        .withStatsReceiver(MetricsStatsReceiver(metricRegistry))
        .serve(s":${opsServerConfig.port}", buildServices)
    )
    val release = (service: ListeningServer) => futureToAsync[IO, Unit](service.close())

    Resource.make(acquire)(release)
  }

  private def buildServices(
      implicit
      metricRegistry: MetricRegistry,
      cs: ContextShift[IO]
  ): Service[Request, Response] = {
    implicit def encodeExceptionCirce: Encoder[Exception] = ErrorHandler.encodeExceptionCirce
    val loggingFilter                                     = new LoggingFilter[Request](TwitterLogger("http.access"), new CommonLogFormatter)
    val endpoints                                         = new OpsEndpoints()

    loggingFilter andThen Bootstrap
      .serve[Application.Json](endpoints.jsonEndpoints.handle(ErrorHandler.apiErrorHandlerAndLogger))
      .serve[Text.Plain](endpoints.plainTextEndpoints.handle(ErrorHandler.apiErrorHandlerAndLogger))
      .toService
  }
}
