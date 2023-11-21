package org.{{ cookiecutter.project_subdomain }}.com

import cats.effect._
import com.typesafe.scalalogging.LazyLogging

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      env    <- getEnvironment
      wiring <- Application.run(env)
    } yield wiring
  }

  private def getEnvironment: IO[String] = {
    System.getenv("ENVIRONMENT") match {
      case null => throw new Exception(s"Could not find required property: ENVIRONMENT.")
      case env  => IO(env)
    }
  }

}

