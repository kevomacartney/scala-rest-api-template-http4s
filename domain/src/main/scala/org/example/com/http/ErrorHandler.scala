package org.example.com.http

import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import io.circe.syntax._
import io.finch
import io.finch.{BadRequest, Output}

object ErrorHandler extends LazyLogging {
  val apiErrorHandlerAndLogger: PartialFunction[Throwable, Output[Nothing]] = {
    case ex: finch.Error =>
      logger.warn(s"(client error) ${ex.getMessage}")
      BadRequest(ex)

    case ex: finch.Errors =>
      logger.warn(s"(client error) ${ex.getMessage}")
      BadRequest(ex)
  }

  val encodeExceptionCirce: Encoder[Exception]                              = Encoder.instance(e =>
    ErrorResponse(
      message = Option(e.getMessage).getOrElse("Encountered an error without a message available")
    ).asJson
  )
}
