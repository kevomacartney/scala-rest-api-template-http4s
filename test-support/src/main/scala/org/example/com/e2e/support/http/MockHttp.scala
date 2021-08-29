package org.example.com.e2e.support.http

import cats.effect.{IO, Resource}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, ListeningServer, Service}
import io.catbird.util.effect.futureToAsync

import java.net.ServerSocket

object MockHttp {
  def withHttpServerServiceAndPort[T](service: Service[Request, Response])(fn: Int => T): T = {
    val freePort = getFreePort
    val acquire  = IO(Http.serve(s":$freePort", service))
    val release  = (server: ListeningServer) => futureToAsync[IO, Unit](server.close())

    Resource
      .make(acquire)(release)
      .use { service =>
        IO(fn(freePort))
      }
      .unsafeRunSync()
  }

  protected def getFreePort: Int = {
    val socket = new ServerSocket(0)
    socket.setReuseAddress(true)

    val localPort = socket.getLocalPort
    socket.close()

    localPort
  }
}
