package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event

trait Event[A] {
  def at: Long
}
