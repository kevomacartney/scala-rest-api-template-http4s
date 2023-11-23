package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web

import cats.effect._
import com.codahale.metrics.MetricRegistry
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io._

class RestService(repository: Repository[IO])(implicit metricRegistry: MetricRegistry) {
  implicit val itemRepositoryEncoder: Encoder[RepositoryItem] = deriveEncoder[RepositoryItem]

  def helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "get" / passCriteria =>
      metricRegistry.meter("retrievals").mark()

      repository.get(passCriteria).flatMap {
        case None           => BadRequest()
        case Some(repoItem) => Ok(repoItem.asJson)
      }
  }

}
