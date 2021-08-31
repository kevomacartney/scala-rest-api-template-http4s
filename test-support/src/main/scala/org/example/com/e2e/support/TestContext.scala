package org.example.com.e2e.support
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.{MediaType, Request, Response}
import org.http4s.Method._
import cats.effect.{IO, Resource}
import org.example.com.e2e.support.TestContext._
import org.http4s.blaze.client.BlazeClientBuilder
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.global

final case class TestContext(serverPort: Int) {
  val serverUrl = s"127.0.0.1:$serverPort"

  def executeRequest(request: Request[IO]): Response[IO] =  ???
}

object TestContext {
  import org.http4s.blaze.client._
  import org.http4s.client._

  protected def withClient[T](f: Client[IO] => T) = ???
}
