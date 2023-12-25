package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql

import com.typesafe.scalalogging.LazyLogging
import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.ChaosContainer
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql.PostgresqlSupport.LatencyToxiName
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.testcontainers.containers.Network

import java.util.UUID

case class PostgresqlTestContext(
    host: String,
    username: String,
    password: String,
    databaseName: String,
    postgresqlTestClient: PostgresqlTestClient
)

trait PostgresqlSupport extends Suite with BeforeAndAfterAll with BeforeAndAfterEach with LazyLogging {
  private val toxiListenPort    = 8667
  private val hostName          = "postgresql"
  private val postgresqlNetwork = Network.newNetwork

  private val toxiproxy         = ChaosContainer.make(postgresqlNetwork)
  val postgresqlToxiproxyClient = new ToxiproxyClient(toxiproxy.getHost, toxiproxy.getControlPort)
  val postgresqlProxy           = postgresqlToxiproxyClient.createProxy(hostName, s"0.0.0.0:$toxiListenPort", s"$hostName:5432")

  private val databaseName        = UUID.randomUUID().toString
  private val postgresqlContainer = PostgresqlContainer.make(postgresqlNetwork, hostName, databaseName)
  val postgresqlTestClient: PostgresqlTestClient =
    PostgresqlTestClient(s"$createProxilessHostUrl/$databaseName", "postgres", "postgres")

  def withPostgresql[A](f: PostgresqlTestContext => A): A = {
    val postgresqlContext = PostgresqlTestContext(
      s"$createToxicPostgresqlHost",
      "postgres",
      "postgres",
      databaseName,
      postgresqlTestClient
    )
    f(postgresqlContext)
  }

  protected override def beforeEach(): Unit = {
    super.beforeEach()
    reEstablishPostgresqlConnection()
  }

  protected def breakPostgresqlConnection(): Unit = {
    postgresqlProxy.disable()
    postgresqlToxiproxyClient.reset()
  }

  protected def reEstablishPostgresqlConnection(): Unit = {
    if (!postgresqlProxy.isEnabled)
      postgresqlProxy.enable()
  }

  protected def addLatencyToxi(latencyMs: Int = 1000): Unit = {
    postgresqlProxy.toxics().latency(LatencyToxiName, ToxicDirection.UPSTREAM, latencyMs)

  }

  protected def removeLatencyToxi(): Unit = {
    postgresqlProxy.toxics().get(LatencyToxiName).remove()
  }

  def createToxicPostgresqlHost: String = {
    val host = toxiproxy.getHost
    val port = toxiproxy.getMappedPort(toxiListenPort)

    s"jdbc:postgresql://$host:$port"
  }

  private def createProxilessHostUrl: String = {
    val host = postgresqlContainer.getHost
    val port = postgresqlContainer.getMappedPort(5432)

    s"jdbc:postgresql://$host:$port"
  }
}

object PostgresqlSupport {
  protected val LatencyToxiName = "UPSTREAM_LATENCY_TOXIC"
}
