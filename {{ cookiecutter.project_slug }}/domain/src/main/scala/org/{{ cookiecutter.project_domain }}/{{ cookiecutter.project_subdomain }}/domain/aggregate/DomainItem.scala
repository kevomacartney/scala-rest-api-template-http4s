package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate

import java.util.UUID

final case class DomainItem(name: String, id: UUID) extends Aggregate[UUID]