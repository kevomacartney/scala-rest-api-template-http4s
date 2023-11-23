package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import scala.io.{BufferedSource, Source}

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    val application = for {
      env     <- Resource.eval(getEnvironment)
      secrets <- loadSecrets
      wiring  <- Application.run(env, secrets)
    } yield wiring

    application
      .use(IO.pure)
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

    val acquire            = IO(Source.fromFile(this.getClass.getResource("/secrets.json").getPath))
    val release            = (bS: BufferedSource) => IO(bS.close())
    val fileBufferResource = Resource.make(acquire)(release)

    for {
      buffer <- fileBufferResource
      str    = buffer.getLines().seq.mkString("\n")
      json   = decode[Map[String, String]](str).getOrElse(throw new RuntimeException("Invalid secrets file"))
    } yield json
  }
}
