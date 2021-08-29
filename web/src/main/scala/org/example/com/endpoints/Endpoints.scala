package org.example.com.endpoints

import cats.effect.{ContextShift, IO}
import com.twitter.finagle.Service
import com.twitter.finagle.http.filter.{CommonLogFormatter, LoggingFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.logging.{Logger => TwitterLogger}
import io.finch.{Application, Bootstrap}
import org.example.com.http.ErrorHandler
import io.circe.generic.auto._
import io.finch.circe._
import org.example.com.endpoints.ApiEndpoint._

object Endpoints {
  def createServices(apiEndpoint: ApiEndpoint)(implicit cs: ContextShift[IO]): Service[Request, Response] = {
    val loggingFilter = new LoggingFilter[Request](TwitterLogger("http.access"), new CommonLogFormatter)
    val jsonEndpoints = apiEndpoint.endpoint


    loggingFilter andThen Bootstrap
      .serve[Application.Json](jsonEndpoints.handle(ErrorHandler.apiErrorHandlerAndLogger))
      .toService
  }

}
