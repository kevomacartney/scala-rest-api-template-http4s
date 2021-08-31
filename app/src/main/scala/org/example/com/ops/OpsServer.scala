package org.example.com.ops

import cats.data.Kleisli
import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.typesafe.scalalogging.LazyLogging
import io.catbird.util.effect.futureToAsync
import io.circe.Encoder
import org.example.com.config.OpsServerConfig
import org.example.com.http._
import org.example.com.httpClient._
import cats.syntax.all._
import org.http4s.{Request, Response}
import org.http4s.blaze.server._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}

import scala.concurrent.ExecutionContext.global

class OpsServer extends LazyLogging {
  def create(
      opsServerConfig: OpsServerConfig
  )(implicit metricRegistry: MetricRegistry, cs: ContextShift[IO]): Resource[IO, Server] = {
    val builtServices = buildServices

    BlazeServerBuilder[IO](global)
      .bindHttp(opsServerConfig.port, "localhost")
      .withHttpApp(builtServices)
      .resource
  }

  private def buildServices(
      implicit
      metricRegistry: MetricRegistry,
      cs: ContextShift[IO]
  ): Kleisli[IO, Request[IO], Response[IO]] = {
    val opsInstance = new OpsService()
    val services    = opsInstance.plainTextServices <+> opsInstance.jsonServices

    Router("/private" -> services).orNotFound
  }
}
