package org.example.com.endpoints

import cats.effect.IO
import com.codahale.metrics.MetricRegistry
import com.twitter.finagle.http.{Response, Status}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import io.finch.Endpoint.path
import io.finch._
import org.example.com.repositories.{Repository, RepositoryItem}

class ApiEndpoint(repository: Repository[IO], metricRegistry: MetricRegistry)
    extends LazyLogging
    with Endpoint.Module[IO] {

  def endpoint: Endpoint[IO, RepositoryItem] = {
    get("get" :: path[String]) { option: String =>
      metricRegistry.meter("retrievals").mark()
      repository.get(option).map {
        case None           => Output.empty[RepositoryItem](Status.BadRequest)
        case Some(repoItem) => Ok(repoItem)
      }
    }
  }
}

object ApiEndpoint {
  implicit val encodeBody: Encoder[RepositoryItem] = deriveEncoder[RepositoryItem]
}
