package org.example.com.httpClient

import cats.effect._
import com.twitter.finagle._
import com.twitter.finagle.http._
import com.twitter.finagle.http.service.HttpResponseClassifier
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.{util => twitter}

import scala.concurrent.duration.Duration

object HttpClient {
  private implicit class ToTwitterDuration(val duration: Duration) {
    def asTwitter: twitter.Duration = twitter.Duration.fromNanoseconds(duration.toNanos)
  }

  def resource(
      config: HttpClientConfig,
      statsReceiver: StatsReceiver
  ): Resource[IO, Service[Request, Response]] =
    Resource
      .make {
        IO(createClient(config, statsReceiver))
      } { service =>
        IO(service.close())
      }

  def createClient(
      config: HttpClientConfig,
      statsReceiver: StatsReceiver
  ): Service[Request, Response] = {
    val clientBuilder =
      Http.client
        .withLabel(config.label)
        .withDecompression(true)
        .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)
        .withSessionQualifier
        .noFailFast
        .withStatsReceiver(statsReceiver)
        .withHttpStats
        .withSessionPool
        .maxSize(config.settings.maxConnections)
        .withSessionPool
        .maxWaiters(config.settings.maxWaiters)
        .withTransport
        .connectTimeout(config.settings.connectTimeout.asTwitter)
        .withTransport
        .readTimeout(config.settings.readTimeout.asTwitter)
        .withStreaming(config.settings.streamingClient)

    if (config.endpoint.getScheme == "https")
      clientBuilder.withTlsWithoutValidation.newService(config.getHostAndPort)
    else clientBuilder.newService(config.getHostAndPort)
  }
}
