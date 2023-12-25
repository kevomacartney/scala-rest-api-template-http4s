package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config

import scala.concurrent.duration.Duration

final case class ApplicationConfig(
    opsServerConfig: OpsServerConfig,
    restConfig: RestApiConfig,
    postgresqlConfig: PostgresqlConfig,
    kafkaConfig: KafkaConfig
)

final case class RestApiConfig(port: Int, idleTimeout: Duration)
final case class OpsServerConfig(port: Int)
final case class PostgresqlConfig(url: String, user: String, password: String, database: String, queryTimeoutMs: Int)
final case class KafkaConfig(
    bootstrapServers: String,
    schemaRegistryUrl: String,
    domainItemEventsTopic: String,
    producerName: String
)
