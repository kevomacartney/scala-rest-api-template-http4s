package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops

import cats.effect._
import com.fasterxml.jackson.databind.ObjectMapper
import io.circe.Encoder.encodeJson
import io.circe.parser._
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._

import java.util.concurrent.TimeUnit

class OpsService(implicit meterRegistry: PrometheusMeterRegistry) {
  lazy val plainTextServices: HttpRoutes[IO] = alive
  lazy val metricsRoute: HttpRoutes[IO]      = appMetrics

  private def alive: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "alive.txt" =>
      Ok(s"I'm alive!")
  }

  private def appMetrics: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metrics" =>
      val prometheusMetrics = meterRegistry.scrape() // TODO handle error

      Ok(prometheusMetrics)
//      parse(metricsJson) match {
//        case Right(json)     => Ok(json)
//        case Left(exception) => InternalServerError(exception.message)
//      }
  }
}
