package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import io.circe.Encoder.encodeJson
import io.circe.parser._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._

import java.util.concurrent.TimeUnit

class OpsService(implicit metrics: MetricRegistry) {
  lazy val plainTextServices: HttpRoutes[IO] = alive
  lazy val jsonServices: HttpRoutes[IO]      = appMetrics

  private val jsonMapperForMetrics: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))
  }

  private def alive: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "alive.txt" =>
      Ok(s"I'm alive!")
  }

  private def appMetrics: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metrics" =>
      val metricsJson = jsonMapperForMetrics.writeValueAsString(metrics)

      parse(metricsJson) match {
        case Right(json)     => Ok(json)
        case Left(exception) => InternalServerError(exception.message)
      }
  }
}
