package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao

import java.util.UUID

case class DomainItemDTO(id: UUID, name: String)
