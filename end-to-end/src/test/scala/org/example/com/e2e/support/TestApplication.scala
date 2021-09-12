package org.example.com.e2e.support

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe.{ParsingFailure, jawn, Json => CirceJson}
import org.example.com.Application
import org.http4s._
import org.http4s.circe.jsonDecoder
import org.scalatest.Assertion
import org.scalatest.concurrent._
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait TestApplication extends ScalaFutures with IntegrationPatience with Matchers with Eventually {
  private val ServerPort    = 5024
  private val OpsServerPort = 5002

  private val healthyTimeout      = 3.seconds
  private val healthCheckInterval = 100.milliseconds

  def withTestApp[T]()(fn: TestContext => T): T = {
    implicit val context: TestContext = TestContext(serverPort = ServerPort)

    val startedServer = Application.run("Acceptance").start.unsafeRunSync()

    val testIo = for {
      _      <- waitForAlive
      _      <- waitForHealthy()
      result <- IO(fn(context))
    } yield result

    testIo
      .flatMap(t => startedServer.cancel >> IO(t))
      .unsafeRunSync()
  }

  protected def fetchMetrics()(implicit context: TestContext): CirceJson = {
    val response = context.executeRequest(makeOpsServerRequest("/private/metrics"))
    response.as[CirceJson].unsafeRunSync()
  }

  private def waitForAlive(implicit context: TestContext): IO[Assertion] =
    eventually {
      IO(context.executeRequest(makeOpsServerRequest("/private/alive.txt")))
        .map { response =>
          response.status.code mustBe Status.Ok.code
        }
    }

  private def ensureHealthy(implicit context: TestContext): IO[Unit] = {
    IO(context.executeRequest(makeOpsServerRequest("/private/healthcheck")))
      .flatMap { response =>
        response.status match {
          case Status.Ok => IO.unit
          case _         => IO.raiseError(new RuntimeException("exhausted retries waiting for healthy application"))
        }
      }
  }

  protected def makeOpsServerRequest(path: String, method: Method = Method.GET): Request[IO] = {
    val fullURl              = s"http://127.0.0.1:$OpsServerPort$path"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullURl))
    request
  }

  private def waitForHealthy(timeout: FiniteDuration = healthyTimeout)(implicit context: TestContext): IO[Unit] = {
    ensureHealthy.handleErrorWith { error =>
      if (timeout.toMillis > 0)
        IO.sleep(healthCheckInterval) *> waitForHealthy(healthyTimeout - healthCheckInterval)
      else
        IO.raiseError(error)
    }
  }
}
