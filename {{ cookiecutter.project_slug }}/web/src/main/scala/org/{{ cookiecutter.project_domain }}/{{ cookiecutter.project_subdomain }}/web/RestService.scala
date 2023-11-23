<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/web/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/web/RestService.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web
========
package org.{{ cookiecutter.project_subdomain }}.com.services
>>>>>>>> main:{{ cookiecutter.project_slug }}/web/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/services/ApiService.scala

import cats.effect._
import com.codahale.metrics.MetricRegistry
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/web/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/web/RestService.scala
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories._
========
import org.{{ cookiecutter.project_subdomain }}.com.ItemRepository
import org.{{ cookiecutter.project_subdomain }}.com.repositories.RepositoryItem
>>>>>>>> main:{{ cookiecutter.project_slug }}/web/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/services/ApiService.scala
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
