package org.example.com.e2e.support

import cats.effect.{IO, Resource}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Http, Service}
import io.catbird.util.effect.futureToAsync
import org.example.com.e2e.support.TestContext._

final case class TestContext(serverPort: Int) {
  val serverUrl = s"127.0.0.1:$serverPort"

  def executeRequest(path: String, method: Method = Method.Get): Response = {
    val request = Request(method, path)
    request.host = serverUrl

    executeRequest(request)
  }

  def executeRequest(request: Request): Response = {
    createClient(request.host.get)
      .use { service =>
        futureToAsync[IO, Response](service(request))
      }
      .unsafeRunSync()
  }
}

object TestContext {
  private def createClient(serverUrl: String): Resource[IO, Service[Request, Response]] = {
    val acquire = IO(Http.newService(serverUrl))
    val release = (svc: Service[Request, Response]) => futureToAsync[IO, Unit](svc.close())

    Resource.make(acquire)(release)
  }
}
