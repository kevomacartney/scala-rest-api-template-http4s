package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao

import cats.Applicative
import cats.data._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxEitherId
import io.micrometer.core.instrument.{MeterRegistry, Tags}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao.{DomainItemDTO, DomainItemDao}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.RepositoryError.UnexpectedRepositoryError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues

import java.util.UUID

class ItemRepositorySpec extends AnyWordSpec with Matchers with ScalaFutures with MockFactory with EitherValues {
  import ItemRepositorySpec._

  "ItemRepository" should {

    "should return a domain item for valid id" in {
      implicit val domainItemDao: DomainItemDao[IO]   = mock[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = mock[MetricsService[IO]]

      val expectedDomainItem = DomainItemDTO(id = UUID.randomUUID(), name = "valid-name")
      (domainItemDao.get _)
        .expects(expectedDomainItem.id)
        .returning(OptionT.apply(IO(Option(expectedDomainItem))))
        .once()

      (metricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .expects(DomainItemDaoGetMetric, Tags.empty(), *)
        .returning(IO.unit)
        .once()

      withRepository { repository =>
        val item = repository.get(expectedDomainItem.id.toString).value.unsafeRunSync()
        item shouldBe Some(DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id)).asRight
      }
    }

    "should return nothing for invalid id" in {
      implicit val domainItemDao: DomainItemDao[IO]   = mock[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = mock[MetricsService[IO]]

      (metricsService.incrementCounter(_: Metric, _: Tags)(_: Applicative[IO])).expects(*, *, *).never()

      withRepository { repository =>
        val item = repository.get("failure").value.unsafeRunSync()
        item.left.value shouldBe a[UnexpectedRepositoryError]
      }
    }

    "should update a domainItem" in {
      implicit val domainItemDao: DomainItemDao[IO]   = stub[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = mock[MetricsService[IO]]

      val expectedDomainItem = DomainItemDTO(id = UUID.randomUUID(), name = "valid-name")
      (domainItemDao.update _).when(expectedDomainItem).returns(IO(expectedDomainItem)).once()

      (metricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .expects(DomainItemDaoUpdateMetric, Tags.empty(), *)
        .returning(IO.unit)
        .once()

      withRepository { repository =>
        val item = repository
          .commitUpdate(DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id))
          .value
          .unsafeRunSync()

        item shouldBe DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id).asRight
      }
    }

    "should return a failure when update fails, and record metrics" in {
      implicit val domainItemDao: DomainItemDao[IO]   = stub[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = stub[MetricsService[IO]]

      val expectedDomainItem = DomainItemDTO(id = UUID.randomUUID(), name = "valid-name")
      (domainItemDao.update _)
        .when(expectedDomainItem)
        .returns(IO.raiseError(new Throwable("failure")))
        .once()

      (metricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .when(DomainItemDaoFailureMetric, Tags.of("cause", "Throwable", "operation", "update"), *)
        .returns(IO.unit)
        .once()

      withRepository { repository =>
        val item = repository
          .commitUpdate(DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id))
          .value
          .unsafeRunSync()

        item.left.value shouldBe a[UnexpectedRepositoryError]
      }
    }

    "should insert a domainItem" in {
      implicit val domainItemDao: DomainItemDao[IO]   = stub[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = mock[MetricsService[IO]]

      val expectedDomainItem = DomainItemDTO(id = UUID.randomUUID(), name = "valid-name")
      (domainItemDao.insert _).when(expectedDomainItem).returns(IO.unit).once()

      (metricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .expects(DomainItemDaoInsertMetric, Tags.empty(), *)
        .returning(IO.unit)
        .once()

      withRepository { repository =>
        val item = repository
          .insert(DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id))
          .value
          .unsafeRunSync()

        item shouldBe DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id).asRight
      }
    }

    "should return a failure when insert fails, and record metrics" in {
      implicit val domainItemDao: DomainItemDao[IO]   = stub[DomainItemDao[IO]]
      implicit val metricsService: MetricsService[IO] = stub[MetricsService[IO]]

      val expectedDomainItem = DomainItemDTO(id = UUID.randomUUID(), name = "valid-name")
      (domainItemDao.insert _)
        .when(expectedDomainItem)
        .returns(IO.raiseError(new Throwable("failure")))
        .once()

      (metricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .when(DomainItemDaoFailureMetric, Tags.of("cause", "Throwable", "operation", "insert"), *)
        .returns(IO.unit)
        .once()

      withRepository { repository =>
        val item = repository
          .insert(DomainItem(name = expectedDomainItem.name, id = expectedDomainItem.id))
          .value
          .unsafeRunSync()

        item.left.value shouldBe a[UnexpectedRepositoryError]
      }
    }
  }
}

object ItemRepositorySpec {
  implicit val metricsRegistry: MeterRegistry = new SimpleMeterRegistry()

  def withRepository[T](
      f: ItemRepository => T
  )(implicit metricsService: MetricsService[IO], domainItemDao: DomainItemDao[IO]) = {
    val repo = new ItemRepository(domainItemDao)
    f(repo)
  }
}
