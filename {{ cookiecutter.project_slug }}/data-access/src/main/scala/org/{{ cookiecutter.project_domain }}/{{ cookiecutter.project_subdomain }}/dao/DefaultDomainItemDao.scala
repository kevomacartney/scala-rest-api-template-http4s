package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao

import cats.data.OptionT
import cats.effect._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.DefaultDomainItemDao._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao.{DomainItemDTO, DomainItemDao}

import java.util.UUID
import scala.concurrent.duration.DurationInt

class DefaultDomainItemDao(transactor: Transactor[IO], queryTimeMs: Int) extends DomainItemDao[IO] {

  override def get(id: UUID): OptionT[IO, DomainItemDTO] = {
    OptionT {
      getByIdQuery(id).option
        .transact(transactor)
        .timeoutAndForget(queryTimeMs.millis)
    }
  }

  override def insert(domainItem: DomainItemDTO): IO[Unit] = {
    insertQuery(domainItem).run
      .transact(transactor)
      .timeoutAndForget(queryTimeMs.millis)
      .map(_ => ())
  }

  override def update(domainItem: DomainItemDTO): IO[DomainItemDTO] = {
    updateQuery(domainItem).run
      .transact(transactor)
      .timeoutAndForget(queryTimeMs.millis)
      .map(_ => domainItem)
  }
}

object DefaultDomainItemDao {
  def apply(
      transactor: Transactor[IO],
      queryTimeout: Int
  ): Resource[IO, DefaultDomainItemDao] = {
    Resource.pure(new DefaultDomainItemDao(transactor, queryTimeout))
  }

  private def getByIdQuery(id: UUID): doobie.Query0[DomainItemDTO] = {
    sql"""
      SELECT id, name
      FROM domain_item_v1
      WHERE id = $id
    """.query[DomainItemDTO]
  }

  private def insertQuery(domainItem: DomainItemDTO): doobie.Update0 = {
    sql"""
      INSERT INTO domain_item_v1 (id, name)
      VALUES (${domainItem.id}, ${domainItem.name})
    """.update
  }

  private def updateQuery(domainItem: DomainItemDTO): doobie.Update0 = {
    sql"""
      UPDATE domain_item_v1
      SET name = ${domainItem.name}
      WHERE id = ${domainItem.id}
    """.update
  }
}
