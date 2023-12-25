package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate

trait Aggregate[ID] {
  def id: ID
}
