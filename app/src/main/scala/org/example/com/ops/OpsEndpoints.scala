package org.example.com.ops

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.finagle.http.{Response, Status}
import com.twitter.io.Buf
import com.typesafe.scalalogging.LazyLogging
import io.finch.Endpoint

import java.util.concurrent.TimeUnit

class OpsEndpoints(implicit cs: ContextShift[IO], metrics: MetricRegistry)
    extends Endpoint.Module[IO]
    with LazyLogging {

  private val jsonMapperForMetrics: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))
  }

  private def alive: Endpoint[IO, Response] = {
    get("private" :: "alive.txt") {
      val response = Response()
      response.contentType = "text/plain"
      response.content = Buf.Utf8("I'm alive!")

      response
    }
  }

  private def healthCheck: Endpoint[IO, Response] = {
    get("private" :: "healthcheck") {
      Response(Status.Ok)
    }
  }

  private def version: Endpoint[IO, Response] = {
    get("private" :: "version") {
      Response()
    }
  }

  private def metricsEndpoint: Endpoint[IO, Response] = {
    get("private" :: "metrics") {
      val metricsJson = jsonMapperForMetrics.writeValueAsString(metrics)

      val response = Response()
      response.contentType = "application/json"
      response.content = Buf.Utf8(metricsJson)

      response
    }
  }

  val plainTextEndpoints = alive :+: version :+: healthCheck

  val jsonEndpoints = metricsEndpoint
}
