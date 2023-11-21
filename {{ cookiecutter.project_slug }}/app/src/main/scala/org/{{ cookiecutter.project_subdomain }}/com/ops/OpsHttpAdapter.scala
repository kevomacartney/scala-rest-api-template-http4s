package org.{{ cookiecutter.project_subdomain }}.com.ops

import cats.data.Kleisli
import cats.effect._
import cats.syntax.all._
import com.codahale.metrics.MetricRegistry
import com.comcast.ip4s._
import com.typesafe.scalalogging.LazyLogging
import org.foxi.com.config.OpsServerConfig
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.{Request, Response}

class OpsHttpAdapter extends LazyLogging {
  def create(opsServerConfig: OpsServerConfig)(implicit metricRegistry: MetricRegistry): Resource[IO, Server] = {
    val builtServices = buildServices

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(opsServerConfig.port).get)
      .withHttpApp(builtServices)
      .build
  }

  private def buildServices(implicit metricRegistry: MetricRegistry): Kleisli[IO, Request[IO], Response[IO]] = {
    val opsInstance = new OpsService()
    val services    = opsInstance.plainTextServices <+> opsInstance.jsonServices

    Router("/private" -> services).orNotFound
  }
}
