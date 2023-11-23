<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/ops/OpsHttpAdapter.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops
========
package org.{{ cookiecutter.project_subdomain }}.com.ops
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/ops/OpsHttpAdapter.scala

import cats.data.Kleisli
import cats.effect._
import cats.syntax.all._
import com.codahale.metrics.MetricRegistry
import com.comcast.ip4s._
import com.typesafe.scalalogging.LazyLogging
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/ops/OpsHttpAdapter.scala
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config.OpsServerConfig
========
import org.foxi.com.config.OpsServerConfig
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/ops/OpsHttpAdapter.scala
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
