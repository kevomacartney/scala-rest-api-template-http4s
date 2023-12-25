package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}

import cats.effect._
import com.comcast.ip4s._
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import fs2.kafka.producer.MkProducer
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroSerializer}
import fs2.kafka.{KafkaByteProducer, KafkaProducer, ProducerSettings}
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm._
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.{{ cookiecutter.project_domain }}.account.ItemRequested
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config.ApplicationConfig
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.publisher.ItemEventPublisher
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.{DefaultDomainItemDao, ItemRepository}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.MetricsService
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops.OpsHttpAdapter
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web.endpoints.RestService
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.server.middleware.{Logger, Metrics}
import org.http4s.server.{Router, Server}
import vulcan.Codec
import vulcan.generic.MagnoliaCodec

import scala.jdk.CollectionConverters.MapHasAsJava

object Wiring extends LazyLogging {

  def initialise(
      appConfig: ApplicationConfig
  )(implicit meterRegistry: PrometheusMeterRegistry): Resource[IO, (Server, Server)] = {
    logger.info("Starting app")

    for {
      postgresqlUrl <- Resource.pure(s"${appConfig.postgresqlConfig.url}/${appConfig.postgresqlConfig.database}")
      transactor <- createTransactor(
                     jdbcUrl = postgresqlUrl,
                     username = appConfig.postgresqlConfig.user,
                     password = appConfig.postgresqlConfig.password,
                     meterRegistry = meterRegistry
                   )

      metricsService = MetricsService[IO](meterRegistry)
      _              = registerJvmMetrics(meterRegistry)

      domainItemDao  <- DefaultDomainItemDao(transactor, appConfig.postgresqlConfig.queryTimeoutMs)
      itemRepository <- ItemRepository(domainItemDao)(metricsService)

      kafkaProducer <- createKafkaProducer(
                        appConfig.kafkaConfig.bootstrapServers,
                        appConfig.kafkaConfig.schemaRegistryUrl,
                        appConfig.kafkaConfig.producerName
                      )

      itemEventPublisher = ItemEventPublisher(kafkaProducer, appConfig.kafkaConfig.domainItemEventsTopic)(
        metricsService
      )
      api <- createRestService(itemRepository, itemEventPublisher, appConfig)(meterRegistry, metricsService)

      ops <- new OpsHttpAdapter().create(appConfig.opsServerConfig)(meterRegistry)
    } yield (api, ops)

  }

  private def createRestService(
      repository: ItemRepository,
      eventPublisher: ItemEventPublisher,
      appConfig: ApplicationConfig
  )(
      implicit metricRegistry: PrometheusMeterRegistry,
      metricsService: MetricsService[IO]
  ): Resource[IO, Server] = {
    val httpService = new RestService(repository, eventPublisher)

    val routes = Router("/" -> httpService.getDomainItemService)

    val meteredStubRoutes = for {
      metrics                <- Prometheus.metricsOps[IO](metricRegistry.getPrometheusRegistry, "http4s")
      meteredRoutes          = Metrics[IO](metrics)(routes)
      loggedAndMeteredRoutes = Logger.httpRoutes[IO](logHeaders = false, logBody = false)(meteredRoutes)
    } yield loggedAndMeteredRoutes

    meteredStubRoutes.flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(Port.fromInt(appConfig.restConfig.port).get)
        .withIdleTimeout(appConfig.restConfig.idleTimeout)
        .withHttpApp(routes.orNotFound)
        .build
    }
  }

  private def registerJvmMetrics(meterRegistry: MeterRegistry): Unit = {
    new ClassLoaderMetrics().bindTo(meterRegistry)
    new JvmMemoryMetrics().bindTo(meterRegistry)
    new JvmGcMetrics().bindTo(meterRegistry)
    new JvmThreadMetrics().bindTo(meterRegistry)
  }

  private def createTransactor(
      jdbcUrl: String,
      username: String,
      password: String,
      meterRegistry: MeterRegistry
  ): Resource[IO, Transactor[IO]] = {
    for {
      hikariConfig <- Resource.pure {
                       val config = new HikariConfig()
                       config.setDriverClassName("org.postgresql.Driver")
                       config.setJdbcUrl(jdbcUrl)
                       config.setUsername(username)
                       config.setPassword(password)
                       config.setMetricRegistry(meterRegistry)
                       config
                     }
      xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig)
    } yield xa
  }

  private def createKafkaProducer(
      bootstrapServers: String,
      schemaRegistryUrl: String,
      producerName: String
  )(implicit meterRegistry: MeterRegistry): Resource[IO, KafkaProducer.Metrics[IO, String, ItemRequested]] = {
    val avroSettings = AvroSettings {
      SchemaRegistryClientSettings[IO](schemaRegistryUrl)
    }
    implicit val codec               = Codec.derive[ItemRequested]
    implicit val eventAvroSerializer = avroSerializer[ItemRequested].forValue(avroSettings)

    val batchSize32kb = 32 * 1024
    val producerSettings = ProducerSettings[IO, String, ItemRequested]
      .withBootstrapServers(bootstrapServers)
      .withBatchSize(batchSize32kb)
      .withClientId(producerName)

    KafkaProducer
      .resource(producerSettings)
      .evalTap(a => a.metrics)
  }

  implicit def customProducerWithMetrics(implicit meterRegistry: MeterRegistry): MkProducer[IO] =
    new MkProducer[IO] {

      def apply[G[_]](settings: ProducerSettings[G, _, _]): IO[KafkaByteProducer] =
        IO {
          val byteArraySerializer = new ByteArraySerializer
          new org.apache.kafka.clients.producer.KafkaProducer(
            (settings.properties: Map[String, AnyRef]).asJava,
            byteArraySerializer,
            byteArraySerializer
          )
        }.flatTap(applyProducerMetrics)

    }

  private def applyProducerMetrics(producer: KafkaByteProducer)(implicit meterRegistry: MeterRegistry): IO[Unit] = {
    IO(new KafkaClientMetrics(producer).bindTo(meterRegistry))
  }
}
