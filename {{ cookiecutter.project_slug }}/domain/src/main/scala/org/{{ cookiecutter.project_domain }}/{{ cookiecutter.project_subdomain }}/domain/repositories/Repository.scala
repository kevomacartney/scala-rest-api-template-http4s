package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.repositories

import cats.data.EitherT
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.RepositoryError

trait Repository[F[_]] {
  def get(id: String): EitherT[F, RepositoryError, Option[DomainItem]]

  def commitUpdate(domainItem: DomainItem): EitherT[F, RepositoryError, DomainItem]

  def insert(domainItem: DomainItem): EitherT[F, RepositoryError, DomainItem]
}
