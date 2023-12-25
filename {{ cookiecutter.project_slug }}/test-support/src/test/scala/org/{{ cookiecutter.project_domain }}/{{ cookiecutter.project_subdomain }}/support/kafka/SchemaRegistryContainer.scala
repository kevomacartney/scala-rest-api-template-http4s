package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka;

import org.testcontainers.containers.{GenericContainer, Network}
import org.testcontainers.utility.DockerImageName

import scala.jdk.CollectionConverters._

object SchemaRegistryContainer {
  val SchemaServerPort = 8081

  def make(network: Network, kafkaHostName: String): GenericContainer[Nothing] = {
    val container: GenericContainer[Nothing] = new GenericContainer(
      DockerImageName.parse("confluentinc/cp-schema-registry:7.1.10")
    )
    container.withNetwork(network)
    container.addExposedPort(SchemaServerPort)
    container.withEnv(
      Map(
        "SCHEMA_REGISTRY_HOST_NAME"                    -> s"${container.getHost}",
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS" -> s"$kafkaHostName:9092"
      ).asJava
    )

    container.start()
    container
  }
}
