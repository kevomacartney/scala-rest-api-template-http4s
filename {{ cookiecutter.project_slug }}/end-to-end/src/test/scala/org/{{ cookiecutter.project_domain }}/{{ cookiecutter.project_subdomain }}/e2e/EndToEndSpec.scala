package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.postgres.implicits._
import doobie.implicits.toSqlInterpolator
import fs2.kafka.ValueDeserializer
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroDeserializer}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e.support.TestApplication
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.featurespec.AnyFeatureSpec
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.aggregate.DomainItem
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.e2e.EndToEndSpec.{getDomainItem, insertDomainItemQuery, withEventDeserializer}
import org.scalatest.matchers.must.Matchers
import org.http4s._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.CollectorRegistry
import org.foxi.account.ItemRequested
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.dao.DomainItemDTO
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.KafkaTestContext
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.postgresql.PostgresqlTestContext
import org.http4s.Method._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import vulcan.Codec
import vulcan.generic.MagnoliaCodec

import java.util.UUID
import scala.concurrent.duration.DurationInt

class EndToEndSpec extends AnyFeatureSpec with TestApplication with ScalaFutures with Matchers {
  implicit val domainItemDecoder = jsonOf[IO, DomainItem]

  Scenario("Server /get responds with expected domain item and events are published") {
    implicit val meterRegistry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    withTestApp() { context =>
      val domainItem  = DomainItem(id = java.util.UUID.randomUUID(), name = "test")
      val insertQuery = insertDomainItemQuery(domainItem)
      context.postgresqlTestContext.postgresqlTestClient.executeQuery(insertQuery)

      context.executeRequestWithResponse(s"/get/${domainItem.id}") { response =>
        response.status shouldBe Status.Ok
        val responseDomainItem = response.as[DomainItem].unsafeRunSync()
        responseDomainItem shouldBe domainItem

        withEventDeserializer { implicit deserializer =>
          val writtenEvent = readLastNKafkaMessages[String, ItemRequested](1, context.kafkaTopic).head

          writtenEvent.record.id shouldBe domainItem.id.toString
          writtenEvent.record.name shouldBe domainItem.name
        }(context.kafkaTestContext)
      }
    }
  }

  Scenario("Server /add writes domain item to database") {
    implicit val meterRegistry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    withTestApp() { implicit context =>
      val domainItem = DomainItem(id = java.util.UUID.randomUUID(), name = "test")

      val request = Request[IO](
        method = PUT,
        uri = Uri.unsafeFromString(s"http://${context.serverUrl}/add")
      ).withEntity(domainItem.asJson)

      context.executeRequestWithResponse(request) { response =>
        response.status shouldBe Status.Ok

        val domainItemFromDb = getDomainItem(domainItem.id, context.postgresqlTestContext)
        domainItemFromDb shouldBe Some(DomainItemDTO(domainItem.id, domainItem.name))
      }
    }
  }

  Scenario("Server /get metrics are recorded") {
    val simpleCollector = new CollectorRegistry(true)
    implicit val meterRegistry: PrometheusMeterRegistry =
      new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, simpleCollector, Clock.SYSTEM)

    withTestApp() { context =>
      val domainItem  = DomainItem(id = java.util.UUID.randomUUID(), name = "test")
      val insertQuery = insertDomainItemQuery(domainItem)
      context.postgresqlTestContext.postgresqlTestClient.executeQuery(insertQuery)

      context.executeRequestWithResponse(s"/get/${domainItem.id}")(_ => ())

      eventually(timeout(5.seconds)) {
        meterRegistry.getPrometheusRegistry.getSampleValue("custom_db_domain_item_v1_get_total") shouldBe 1.0

        meterRegistry.getPrometheusRegistry
          .getSampleValue(
            "http4s_request_count_total",
            Array("classifier", "method", "status"),
            Array("", "get", "2xx")
          ) shouldBe 1.0

        meterRegistry.getPrometheusRegistry
          .getSampleValue(
            "kafka_producer_record_send_total",
            Array("client_id", "kafka_version"),
            Array("domain-item-producer", "7.5.1-ccs")
          ) shouldBe 1
      }
    }
  }
}

object EndToEndSpec {
  def withEventDeserializer[T](
      f: ValueDeserializer[IO, ItemRequested] => T
  )(implicit kafkaContext: KafkaTestContext): T = {
    implicit val codec: Codec[ItemRequested] = Codec.derive[ItemRequested]
    val avroSettings = AvroSettings {
      SchemaRegistryClientSettings[IO](kafkaContext.schemaServer)
    }.withAutoRegisterSchemas(false)

    avroDeserializer[ItemRequested]
      .forValue(avroSettings)
      .use { deserializer =>
        IO(f(deserializer))
      }
      .unsafeRunSync()
  }

  def insertDomainItemQuery(
      domainItem: DomainItem
  ): doobie.ConnectionIO[Int] = {
    sql"""
             INSERT INTO domain_item_v1 (id, name)
             VALUES (${domainItem.id}, ${domainItem.name});
             """.update.run
  }

  def getDomainItem(id: UUID, postgresqlContext: PostgresqlTestContext): Option[DomainItemDTO] = {
    val getByIdQuery =
      sql"""
             SELECT id, name
             FROM domain_item_v1
             WHERE id = $id
             """.query[DomainItemDTO]

    postgresqlContext.postgresqlTestClient.executeQuery[Option[DomainItemDTO]](getByIdQuery.option)
  }
}
