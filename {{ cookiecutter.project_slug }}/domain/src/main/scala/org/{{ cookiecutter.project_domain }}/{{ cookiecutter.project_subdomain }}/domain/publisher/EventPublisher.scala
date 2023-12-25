package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.publisher

import fs2._

trait EventPublisher[F[_], A] {
  def publish: Pipe[F, A, Unit]
}
