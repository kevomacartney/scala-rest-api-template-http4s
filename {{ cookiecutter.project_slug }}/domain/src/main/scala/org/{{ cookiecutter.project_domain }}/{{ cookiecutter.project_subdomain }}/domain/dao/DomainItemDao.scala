package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao

import cats.data.OptionT
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem

import java.util.UUID

trait DomainItemDao[F[_]] {
  def get(id: UUID): OptionT[F, DomainItemDTO]

  def insert(domainItem: DomainItemDTO): F[Unit]

  def update(domainItem: DomainItemDTO): F[DomainItemDTO]
}
