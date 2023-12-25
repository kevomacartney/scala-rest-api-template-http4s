package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka

import com.github.dockerjava.api.command.InspectContainerResponse
import org.testcontainers.containers.{KafkaContainer, Network}
import org.testcontainers.images.builder.Transferable
import org.testcontainers.shaded.com.google.common.base.Supplier
import org.testcontainers.utility.DockerImageName

import scala.jdk.CollectionConverters._

class KafkaTestContainer(val toxiProxy: String)
    extends KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.3")) {
  private val startScript = "/testcontainers_start.sh"

  override protected def containerIsStarting(containerInfo: InspectContainerResponse): Unit = {
    val kafkaToxiHost       = s"PLAINTEXT://$toxiProxy"
    val advertisedListeners = List(kafkaToxiHost, brokerAdvertisedListener(containerInfo)).asJava

    val kafkaAdvertisedListeners = String.join(",", advertisedListeners)

    var command = "#!/bin/bash\n"

    command += "echo '' > /etc/confluent/docker/ensure \n"
    command += commandZookeeper

    // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname// exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
    command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s\n", kafkaAdvertisedListeners)

    // Run the original command// Run the original command
    command += "/etc/confluent/docker/run \n"
    copyFileToContainer(Transferable.of(command, 0x1ff), startScript)
  }
}

object KafkaTestContainer {
  def make(network: Network, hostName: String, toxicHost: String): KafkaContainer = {
    val kafkaContainer = new KafkaTestContainer(toxicHost)
    kafkaContainer.withNetwork(network)
    kafkaContainer.withNetworkAliases(hostName)
    kafkaContainer.withEnv(
      Map(
        "KAFKA_BROKER_ID"                 -> "1",
        "KAFKA_HOST_NAME"                 -> hostName,
        "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "false"
      ).asJava
    )

    kafkaContainer.start()
    kafkaContainer
  }
}
