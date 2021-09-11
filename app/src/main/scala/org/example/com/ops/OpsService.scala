package org.example.com.ops

import cats.effect.{ContextShift, IO}
import com.codahale.metrics.MetricRegistry
import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.io.Buf
import org.example.com.ItemRepository
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Type`
import java.util.concurrent.TimeUnit
import cats.syntax.all._
import org.http4s.blaze.server._
import org.http4s.implicits._

class OpsService(implicit cs: ContextShift[IO], metrics: MetricRegistry) {
  val plainTextServices = alive <+> healthCheck
  val jsonServices      = appMetrics
  private val jsonMapperForMetrics: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))
  }
  private val alive: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "alive.txt" =>
      Ok(s"I'm alive!")
  }
  private val healthCheck: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "healthCheck" =>
      Ok()
  }
  private val appMetrics: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "metrics" =>
      val metricsJson = jsonMapperForMetrics.writeValueAsString(metrics)
      Ok(metricsJson, `Content-Type`(MediaType.application.json))
  }
}
