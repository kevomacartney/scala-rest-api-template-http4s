package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops

import cats.data.Kleisli
import cats.effect._
import cats.syntax.all._
import com.comcast.ip4s._
import com.typesafe.scalalogging.LazyLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config.OpsServerConfig
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.{Request, Response}

class OpsHttpAdapter extends LazyLogging {
  def create(opsServerConfig: OpsServerConfig)(implicit meterRegistry: PrometheusMeterRegistry): Resource[IO, Server] = {
    val builtServices = buildServices

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(opsServerConfig.port).get)
      .withHttpApp(builtServices)
      .build
  }

  private def buildServices(implicit meterRegistry: PrometheusMeterRegistry): Kleisli[IO, Request[IO], Response[IO]] = {
    val opsInstance = new OpsService()
    val services    = opsInstance.plainTextServices <+> opsInstance.metricsRoute

    Router("/private" -> services).orNotFound
  }
}
