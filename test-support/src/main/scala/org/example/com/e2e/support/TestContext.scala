package org.example.com.e2e.support

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.client._
import org.http4s._

final case class TestContext(serverPort: Int) {
  val serverUrl              = s"127.0.0.1:$serverPort"
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def executeRequest(url: String, method: Method = Method.GET): Response[IO] = {
    val fullUrl              = s"http://$serverUrl$url"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullUrl))
    executeRequest(request)
  }

  def executeRequest(request: Request[IO]): Response[IO] = {
    httpClient.run(request).use(IO(_)).unsafeRunSync()
  }
}
