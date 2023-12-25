package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web.endpoints

import cats.data.EitherT
import cats.effect._
import cats.effect.unsafe.IORuntime
import cats.implicits._
import fs2.{Pipe, Stream}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.RepositoryError.UnexpectedRepositoryError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event.ItemRequestedEvent
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.MetricsService
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.publisher.EventPublisher
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID

class RestServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockFactory {
  import ApiEndpointSpec._

  implicit val runtime: IORuntime                                         = cats.effect.unsafe.IORuntime.global
  implicit val repositoryItemDecoder: Decoder[DomainItem]                 = deriveDecoder[DomainItem]
  implicit val repositoryItemEntityDecoder: EntityDecoder[IO, DomainItem] = jsonOf[IO, DomainItem]

  "GET/ get" should {
    "returns a repository item" in {
      implicit val mockedRepository: Repository[IO] = mock[Repository[IO]]
      implicit val mockedPublisher: EventPublisher[IO, ItemRequestedEvent] =
        mock[EventPublisher[IO, ItemRequestedEvent]]
      implicit val mockedMetrics: MetricsService[IO] = mock[MetricsService[IO]]

      withService { service =>
        val mockPipe: Pipe[IO, ItemRequestedEvent, Unit] = _.flatMap(_ => Stream.empty.covary[IO])
        (mockedPublisher.publish _).expects().returning(mockPipe)
        (mockedRepository.get _)
          .expects("success")
          .returning(EitherT(IO(DomainItem("success", UUID.randomUUID()).some.asRight)))
          .once()

        val request  = buildSuccessRequest()
        val response = service(request).unsafeRunSync()

        response.status mustBe Status.Ok
        response.as[DomainItem].unsafeRunSync()
      }
    }

    "returns a 500 when repository returns an error" in {
      implicit val mockedRepository: Repository[IO] = mock[Repository[IO]]
      implicit val mockedPublisher: EventPublisher[IO, ItemRequestedEvent] =
        mock[EventPublisher[IO, ItemRequestedEvent]]
      implicit val mockedMetrics: MetricsService[IO] = mock[MetricsService[IO]]

      withService { service =>
        (mockedRepository.get _)
          .expects("failure")
          .returning(EitherT(IO(UnexpectedRepositoryError(new Throwable("failure"), "failure", "get").asLeft)))
          .once()

        val request  = buildFailureRequest()
        val response = service(request).unsafeRunSync()

        response.status mustBe Status.InternalServerError
      }
    }

    "returns a 404 when repository returns None" in {
      implicit val mockedRepository: Repository[IO] = mock[Repository[IO]]
      implicit val mockedPublisher: EventPublisher[IO, ItemRequestedEvent] =
        mock[EventPublisher[IO, ItemRequestedEvent]]
      implicit val mockedMetrics: MetricsService[IO] = mock[MetricsService[IO]]

      withService { service =>
        val mockPipe: Pipe[IO, ItemRequestedEvent, Unit] = _.flatMap(_ => Stream.empty.covary[IO])

        (mockedRepository.get _)
          .expects("failure")
          .returning(EitherT(IO(none.asRight)))
          .once()

        (mockedPublisher.publish _)
          .expects()
          .returning(mockPipe)

        val request  = buildFailureRequest()
        val response = service(request).unsafeRunSync()

        response.status mustBe Status.NotFound
      }
    }
  }
}

object ApiEndpointSpec {
  type Service = Request[IO] => IO[Response[IO]]

  def withService[T](
      f: Service => T
  )(
      implicit repository: Repository[IO],
      eventPublisher: EventPublisher[IO, ItemRequestedEvent],
      metricsService: MetricsService[IO]
  ): T = {
    val service = new RestService(repository, eventPublisher).getDomainItemService.orNotFound.run
    f(service)
  }

  def buildSuccessRequest(): Request[IO] = {
    Request(method = Method.GET, uri = Uri.unsafeFromString("/get/success"))
  }

  def buildFailureRequest(): Request[IO] = {
    Request(method = Method.GET, uri = Uri.unsafeFromString("/get/failure"))
  }
}
