package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao

import cats.data._
import cats.effect._
import cats.implicits._
import io.micrometer.core.instrument.Tags
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.ItemRepository._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao.{DomainItemDTO, DomainItemDao}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.RepositoryError.UnexpectedRepositoryError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.{ErrorHandler, RepositoryError}

import java.util.UUID
import scala.util.Try

class ItemRepository(defaultDomainItemDao: DomainItemDao[IO])(implicit metricsService: MetricsService[IO])
    extends Repository[IO] {
  override def get(id: String): EitherT[IO, RepositoryError, Option[DomainItem]] = {
    Try(UUID.fromString(id)).toOption match {
      case Some(id) => retrieveById(id)
      case _        => EitherT(IO(UnexpectedRepositoryError(new Throwable("failure"), "failure", "get").asLeft))
    }
  }

  override def commitUpdate(domainItem: DomainItem): EitherT[IO, RepositoryError, DomainItem] = {
    EitherT(defaultDomainItemDao.update(mapDomainItemToDomainItemDTO(domainItem)).attempt)
      .leftSemiflatMap(error => handleError(error, domainItem.id.toString, "update"))
      .semiflatTap(_ => metricsService.incrementCounter(DomainItemDaoUpdateMetric, Tags.empty()))
      .map(_ => domainItem)
  }

  private def retrieveById(id: UUID): EitherT[IO, RepositoryError, Option[DomainItem]] = {
    EitherT(defaultDomainItemDao.get(id).value.attempt)
      .leftSemiflatMap(error => handleError(error, id.toString, "get"))
      .semiflatTap(_ => metricsService.incrementCounter(DomainItemDaoGetMetric, Tags.empty()))
      .map(_.map(mapDomainItemDTOToDomainItem))
  }

  private def handleError(error: Throwable, itemId: String, operation: String): IO[RepositoryError] = {
    for {
      error <- IO(
                UnexpectedRepositoryError(
                  cause = error,
                  error = s"Unexpected error from $operation operation item [id=$itemId]",
                  operation = operation
                )
              )
      _ <- ErrorHandler.handle(error)
    } yield error
  }

  override def insert(domainItem: DomainItem): EitherT[IO, RepositoryError, DomainItem] = {
    EitherT(defaultDomainItemDao.insert(mapDomainItemToDomainItemDTO(domainItem)).attempt)
      .leftSemiflatMap(error => handleError(error, domainItem.id.toString, "insert"))
      .semiflatTap(_ => metricsService.incrementCounter(DomainItemDaoInsertMetric, Tags.empty()))
      .map(_ => domainItem)
  }
}

object ItemRepository {
  def apply(
      domainRepository: DomainItemDao[IO]
  )(implicit metricsService: MetricsService[IO]): Resource[IO, ItemRepository] = {
    val acquire = IO(new ItemRepository(domainRepository))
    Resource.eval(acquire)
  }

  private def mapDomainItemToDomainItemDTO(domainItem: DomainItem): DomainItemDTO = {
    DomainItemDTO(
      id = domainItem.id,
      name = domainItem.name
    )
  }

  private def mapDomainItemDTOToDomainItem(domainItemDTO: DomainItemDTO): DomainItem = {
    DomainItem(
      id = domainItemDTO.id,
      name = domainItemDTO.name
    )
  }
}
