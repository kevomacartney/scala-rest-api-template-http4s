package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

import scala.io.{BufferedSource, Source}

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    val application = for {
      env           <- Resource.eval(getEnvironment)
      secrets       <- loadSecrets
      meterRegistry <- new PrometheusMeterRegistry(PrometheusConfig.DEFAULT).pure[Resource[IO, *]]
      wiring        <- Application.run(env, secrets)(meterRegistry)
    } yield wiring

    application
      .use(_ => IO.never.as(ExitCode.Success))
      .handleErrorWith { e =>
        logger.error("Application failed to start", e)
        ExitCode.Error.pure[IO]
      }
      .as(ExitCode.Success)
  }

  private def getEnvironment: IO[String] = {
    System.getenv("ENVIRONMENT") match {
      case null => throw new Exception(s"Could not find required property: ENVIRONMENT.")
      case env  => IO(env)
    }
  }

  private def loadSecrets: Resource[IO, Map[String, String]] = {
    import io.circe.parser._

    val acquire            = IO(Source.fromInputStream(this.getClass().getResourceAsStream("/secrets.json")))
    val release            = (bS: BufferedSource) => IO(bS.close())
    val fileBufferResource = Resource.make(acquire)(release)

    for {
      buffer <- fileBufferResource
      str    = buffer.mkString
      json   = decode[Map[String, String]](str).getOrElse(throw new RuntimeException("Invalid secrets file"))
    } yield json
  }
}
