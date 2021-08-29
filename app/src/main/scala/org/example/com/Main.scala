package org.example.com

import cats.effect._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.example.com.Application.logger
import org.example.com.config.ApplicationConfig
import pureconfig.ConfigSource

import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters.mapAsJavaMapConverter
import scala.util.{Failure, Success, Try}

object Main extends IOApp with LazyLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    for {
      env    <- getEnvironment
      wiring <- Application.run(env)
    } yield wiring
  }

  def getEnvironment: IO[String] = {
    System.getenv("ENVIRONMENT") match {
      case null => throw new Exception(s"Could not find required property: ENVIRONMENT.")
      case env  => IO(env)
    }
  }

}
