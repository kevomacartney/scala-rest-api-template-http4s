package org.example.com.http

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class ErrorResponse(message: String)

object ErrorResponse {
  implicit val decodeErrorResponse: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  implicit val encodeErrorResponse: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}
