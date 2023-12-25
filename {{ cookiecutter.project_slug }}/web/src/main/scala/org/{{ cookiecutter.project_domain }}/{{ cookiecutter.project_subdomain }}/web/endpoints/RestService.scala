package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web.endpoints

import cats.data.EitherT
import cats.effect._
import fs2.{Pipe, Stream}
import io.circe.generic.auto._
import io.circe.syntax._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem

import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.{ErrorHandler, RepositoryError}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event.ItemRequestedEvent
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.MetricsService
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.publisher.EventPublisher
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

class RestService(repository: Repository[IO], eventPublisher: EventPublisher[IO, ItemRequestedEvent])(
    implicit metricsService: MetricsService[IO]
) {
  implicit val domainItemDecoder: EntityDecoder[IO, DomainItem] = jsonOf[IO, DomainItem]

  def getDomainItemService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "get" / id =>
      val itemWithPublish = Stream
        .eval(repository.get(id).value)
        .observe(processEvent)
        .compile
        .lastOrError

      itemWithPublish.flatMap(createResponse)

    case request @ PUT -> Root / "add" =>
      EitherT
        .liftF(request.as[DomainItem])
        .flatMap(repository.insert)
        .foldF(
          error => ErrorHandler.handle(error) *> BadRequest(),
          domain => Ok(domain.asJson)
        )
  }

  private def createEvent(item: DomainItem): ItemRequestedEvent = ItemRequestedEvent(item.id.toString, item.name)

  private def createResponse(either: Either[RepositoryError, Option[DomainItem]]): IO[Response[IO]] = either match {
    case Left(_)               => InternalServerError()
    case Right(None)           => NotFound()
    case Right(Some(repoItem)) => Ok(repoItem.asJson)
  }

  private def processEvent: Pipe[IO, Either[RepositoryError, Option[DomainItem]], Nothing] = _.flatMap {
    case Right(domainItem) =>
      Stream
        .emits(domainItem.map(createEvent).toList)
        .covary[IO]
        .through(eventPublisher.publish)
        .foreach(_ => IO.unit)

    case Left(_) =>
      Stream.empty.covary[IO]
  }
}
