package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka

import org.testcontainers.containers.{Network, ToxiproxyContainer}

object ChaosContainer {
  def make(network: Network): ToxiproxyContainer = {
    val toxiproxy: ToxiproxyContainer = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
      .withNetwork(network)

    toxiproxy.start()
    toxiproxy
  }
}
