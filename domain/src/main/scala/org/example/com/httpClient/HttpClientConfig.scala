package org.example.com.httpClient

import java.net.URI
import scala.concurrent.duration.FiniteDuration

final case class HttpClientConfig(endpoint: URI, label: String, settings: HttpClientSettings) {
  def getHostAndPort: String = s"${endpoint.getHost}:${endpoint.getPort}"
}

final case class HttpClientSettings(
    connectTimeout: FiniteDuration,
    readTimeout: FiniteDuration,
    maxConnections: Int,
    maxWaiters: Int,
    streamingClient: Boolean
)
