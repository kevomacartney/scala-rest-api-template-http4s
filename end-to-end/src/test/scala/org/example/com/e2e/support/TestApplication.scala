package org.example.com.e2e.support

import cats.effect._
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import io.circe.{ParsingFailure, jawn}
import io.finch.Application.Json
import io.finch.Output
import io.finch.catsEffect.{get, path}
import org.example.com.Application
import org.scalatest.Assertion
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import io.circe.{Json => CirceJson}
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait TestApplication extends ScalaFutures with IntegrationPatience with Matchers with Eventually {
  private val ServerPort    = 5024
  private val OpsServerPort = 5002

  private val healthyTimeout      = 3.seconds
  private val healthcheckInterval = 100.milliseconds

  implicit protected def contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit protected def timer: Timer[IO]               = IO.timer(ExecutionContext.global)

  protected def stubHttpServicesNotImplemented: Service[Request, Response] = {
    import io.circe.generic.auto._
    import io.finch.circe._

    get("foo" :: "bar" :: path[UUID])((_: UUID) => Output.empty[String](Status.NotImplemented)).toServiceAs[Json]
  }

  def withTestApp[T]()(fn: TestContext => T): T = {
    implicit val context: TestContext = TestContext(serverPort = ServerPort)

    val testIo = for {
      startedServer <- Application.run("Acceptance").start
      _             <- waitForAlive
      _             <- waitForHealthy()
      result        <- IO(fn(context))
      _             <- startedServer.cancel
    } yield result

    testIo.unsafeRunSync()
  }

  protected def fetchMetrics()(implicit context: TestContext): Either[ParsingFailure, CirceJson] = {
    val response = context.executeRequest(makeOpsServerRequest("/private/metrics"))
    jawn.parse(response.contentString)
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

  private def waitForHealthy(timeout: FiniteDuration = healthyTimeout)(implicit context: TestContext): IO[Unit] = {
    ensureHealthy.handleErrorWith { error =>
      if (timeout.toMillis > 0)
        IO.sleep(healthcheckInterval) *> waitForHealthy(healthyTimeout - healthcheckInterval)
      else
        IO.raiseError(error)
    }
  }

  protected def makeOpsServerRequest(path: String, method: Method = Method.Get): Request = {
    val request = Request(method, path)
    request.host = s"127.0.0.1:$OpsServerPort"
    request
  }
}
