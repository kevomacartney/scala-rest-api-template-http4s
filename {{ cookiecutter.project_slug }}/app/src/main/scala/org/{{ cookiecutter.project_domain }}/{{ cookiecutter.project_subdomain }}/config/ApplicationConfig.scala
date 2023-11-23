package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config

final case class ApplicationConfig(opsServerConfig: OpsServerConfig, restConfig: RestApiConfig)

final case class RestApiConfig(port: Int)
final case class OpsServerConfig(port: Int)
