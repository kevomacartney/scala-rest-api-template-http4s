<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/e2e/support/TestApplication.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e.support
========
package org.{{ cookiecutter.project_subdomain }}.com.e2e.support
>>>>>>>> main:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_subdomain }}/com/e2e/support/TestApplication.scala

import cats.effect._
import cats.effect.unsafe.implicits.global
import io.circe._
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/e2e/support/TestApplication.scala
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.Application
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.e2e.TestContext
========
import org.{{ cookiecutter.project_subdomain }}.com.Application
>>>>>>>> main:{{ cookiecutter.project_slug }}/end-to-end/src/test/scala/org/{{ cookiecutter.project_subdomain }}/com/e2e/support/TestApplication.scala
import org.http4s._
import org.http4s.circe._
import org.scalatest.Assertion
import org.scalatest.concurrent._
import org.scalatest.matchers.must.Matchers

import java.net.ServerSocket
import scala.concurrent.duration.DurationInt

trait TestApplication extends ScalaFutures with IntegrationPatience with Matchers with Eventually {
  private val ServerPort    = freePort
  private val OpsServerPort = freePort

  def withTestApp[T]()(fn: TestContext => T): T = {
    implicit val context: TestContext = TestContext(serverPort = ServerPort)

    Application
      .run("e2e", serverPortsOverride)
      .use { _ =>
        waitForAlive
        IO(fn(context))
      }
      .unsafeRunSync()
  }

  protected def fetchMetrics()(implicit context: TestContext): Json = {
    context.executeRequestWithResponse(makeOpsServerRequest("/private/metrics")) { response =>
      response.as[Json].unsafeRunSync()
    }
  }

  private def waitForAlive(implicit context: TestContext): Assertion =
    eventually(timeout(10.seconds)) {
      context.executeRequestWithResponse(makeOpsServerRequest("/private/alive.txt")) { response =>
        response.status mustBe Status.Ok
      }
    }

  protected def makeOpsServerRequest(path: String, method: Method = Method.GET): Request[IO] = {
    val fullURl              = s"http://127.0.0.1:$OpsServerPort$path"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullURl))
    request
  }

  private def freePort: Int = {
    val socket = new ServerSocket(0)
    val port   = socket.getLocalPort
    socket.close()
    port
  }

  private def serverPortsOverride: Map[String, String] = {
    Map(
      "rest-config.port"       -> ServerPort.toString,
      "ops-server-config.port" -> OpsServerPort.toString
    )
  }
}

