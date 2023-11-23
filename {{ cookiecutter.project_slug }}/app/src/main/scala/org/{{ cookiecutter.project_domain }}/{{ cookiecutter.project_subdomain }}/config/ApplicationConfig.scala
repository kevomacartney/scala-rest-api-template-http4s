<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/config/ApplicationConfig.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config
========
package org.{{ cookiecutter.project_subdomain }}.com.config
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/config/ApplicationConfig.scala

final case class ApplicationConfig(opsServerConfig: OpsServerConfig, restConfig: RestApiConfig)

final case class RestApiConfig(port: Int)
final case class OpsServerConfig(port: Int)
