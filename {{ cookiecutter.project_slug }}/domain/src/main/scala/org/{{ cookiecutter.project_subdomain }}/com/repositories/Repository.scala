package org.{{ cookiecutter.project_subdomain }}.com.repositories

import java.util.UUID
import scala.language.higherKinds

trait Repository[F[_]] {
  def get(id: String): F[Option[RepositoryItem]]
}

final case class RepositoryItem(name: String, id: UUID)