package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e.support

import cats.effect._
import cats.effect.unsafe.implicits.global
import doobie.implicits.toSqlInterpolator
import io.circe._
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.CollectorRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.Application
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.e2e.TestContext
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.{KafkaSupport, KafkaTestContext}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql.{PostgresqlSupport, PostgresqlTestContext}
import org.http4s._
import org.http4s.circe._
import org.scalatest.Assertion
import org.scalatest.concurrent._
import org.scalatest.matchers.must.Matchers

import java.net.ServerSocket
import java.util.UUID
import scala.concurrent.duration.DurationInt

trait TestApplication
//    extends IntegrationPatience
    extends Matchers
    with Eventually
    with PostgresqlSupport
    with KafkaSupport {
  private val ServerPort    = freePort
  private val OpsServerPort = freePort

  def withTestApp[T]()(
      fn: TestContext => T
  )(implicit meterRegistry: PrometheusMeterRegistry): T = {
    val topicName = UUID.randomUUID().toString

    withPostgresql { postgresqlContext =>
      withKafka(topicName) { kafkaContext =>
        implicit val context: TestContext = TestContext(ServerPort, postgresqlContext, kafkaContext, topicName)

        Application
          .run("e2e", serverPortsOverride(postgresqlContext, kafkaContext, topicName))
          .use { _ =>
            for {
              _ <- createDomainItemTable(postgresqlContext)
              _ <- IO(waitForAlive)
              t <- IO(fn(context))
              _ <- dropDomainItemTable(postgresqlContext)
            } yield t
          }
          .unsafeRunSync()
      }
    }
  }

  private def waitForAlive(implicit context: TestContext): Assertion =
    eventually(timeout(10.seconds)) {
      context.executeRequestWithResponse(makeOpsServerRequest("/private/alive.txt")) { response =>
        response.status mustBe Status.Ok
      }
    }

  protected def makeOpsServerRequest(path: String, method: Method = Method.GET): Request[IO] = {
    val fullURl              = s"http://127.0.0.1:$OpsServerPort$path"
    val request: Request[IO] = Request(method, uri = Uri.unsafeFromString(fullURl))
    request
  }

  private def freePort: Int = {
    val socket = new ServerSocket(0)
    val port   = socket.getLocalPort
    socket.close()
    port
  }

  private def serverPortsOverride(
      postgresqlContext: PostgresqlTestContext,
      kafkaContext: KafkaTestContext,
      kafkaTopic: String
  ): Map[String, String] = {
    Map(
      "rest-config.port"                      -> ServerPort.toString,
      "ops-server-config.port"                -> OpsServerPort.toString,
      "postgresql-config.url"                 -> postgresqlContext.host,
      "postgresql-config.database"            -> postgresqlContext.databaseName,
      "postgresql-config.user"                -> postgresqlContext.username,
      "postgresql-config.password"            -> postgresqlContext.password,
      "kafka-config.bootstrap-servers"        -> kafkaContext.boostrapServers,
      "kafka-config.schema-registry-url"      -> kafkaContext.schemaServer,
      "kafka-config.domain-item-events-topic" -> kafkaTopic
    )
  }

  def createDomainItemTable(postgresqlContext: PostgresqlTestContext): IO[Int] = {
    val createTableQuery =
      sql"""
             CREATE TABLE domain_item_v1 (
               id UUID PRIMARY KEY,
               name VARCHAR(255) NOT NULL
             );
             """

    postgresqlContext.postgresqlTestClient.executeQueryAsync(createTableQuery.update.run)
  }

  def dropDomainItemTable(postgresqlContext: PostgresqlTestContext): IO[Int] = {
    val dropTableQuery =
      sql"""
             DROP TABLE domain_item_v1;
             """

    postgresqlContext.postgresqlTestClient.executeQueryAsync(dropTableQuery.update.run)
  }
}
