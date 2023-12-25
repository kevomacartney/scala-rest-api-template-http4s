package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql

import org.testcontainers.containers.{Network, PostgreSQLContainer}

object PostgresqlContainer {
  def make(
      network: Network,
      hostName: String,
      database: String
  ): PostgreSQLContainer[Nothing] = {
    val postgresqlContainer: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres:16.1-alpine3.19")
    postgresqlContainer.withNetwork(network)
    postgresqlContainer.withNetworkAliases(hostName)
    postgresqlContainer.withUsername("postgres")
    postgresqlContainer.withPassword("postgres")
    postgresqlContainer.withDatabaseName(database)

    postgresqlContainer.start()
    postgresqlContainer
  }
}
