package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.scalalogging.LazyLogging
import eu.rekawek.toxiproxy.ToxiproxyClient
import fs2.kafka._
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.SchemaRegistryContainer.SchemaServerPort
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.testcontainers.containers.{GenericContainer, KafkaContainer, Network, ToxiproxyContainer}

import java.util.{Properties, UUID}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters._

case class KafkaTestContext(boostrapServers: String, schemaServer: String)
case class ReadRecord[K, V](key: K, record: V)

trait KafkaSupport extends Suite with BeforeAndAfterAll with BeforeAndAfterEach with LazyLogging {
  private val kafkaHostName: String = s"kafka-${UUID.randomUUID().toString}"
  private val kafkaNetwork          = Network.newNetwork

  private val kafkaToxiProxyContainer: ToxiproxyContainer = ChaosContainer.make(kafkaNetwork)
  val kafkaToxiProxyClient =
    new ToxiproxyClient(kafkaToxiProxyContainer.getHost, kafkaToxiProxyContainer.getControlPort)
  val kafkaToxiProxy = kafkaToxiProxyClient.createProxy(kafkaHostName, "0.0.0.0:8666", s"$kafkaHostName:9093")

  val kafka: KafkaContainer                     = KafkaTestContainer.make(kafkaNetwork, kafkaHostName, toxicHost = makeKafkaToxicHost)
  val schemaRegistry: GenericContainer[Nothing] = SchemaRegistryContainer.make(kafkaNetwork, kafkaHostName)

  def withKafka[T](topicName: String)(f: KafkaTestContext => T): T = {
    createKafkaTopic(topicName)
    val kafkaBootstrapServers = makeBoostrapUrl
    val schemaRegistryUrl     = s"http://localhost:${schemaRegistry.getFirstMappedPort}"

    val kafkaTestContext = KafkaTestContext(kafkaBootstrapServers, schemaRegistryUrl)
    f(kafkaTestContext)
  }

  def readLastNKafkaMessages[K, V](n: Int, topic: String, timeout: FiniteDuration = 15.seconds)(
      implicit valueDeserializer: ValueDeserializer[IO, V],
      keyDeserializer: KeyDeserializer[IO, K]
  ): List[ReadRecord[K, V]] = {
    val kafkaBootstrapServers = kafka.getBootstrapServers
    val groupId               = UUID.randomUUID().toString

    val consumerSettings =
      ConsumerSettings[IO, K, V]
        .withBootstrapServers(kafkaBootstrapServers)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(groupId)
        .withAllowAutoCreateTopics(false)

    KafkaConsumer
      .stream(consumerSettings)
      .subscribeTo(topic)
      .records
      .map(record => ReadRecord(record.record.key, record.record.value))
      .take(n)
      .timeout(timeout)
      .compile
      .toList
      .unsafeRunSync()
  }

  protected def breakKafkaConnection(): Unit = {
    kafkaToxiProxy.disable()
  }

  protected def reEstablishKafkaConnection(): Unit = {
    kafkaToxiProxy.enable()
  }

  private def createKafkaTopic(topicName: String): Unit = {
    val adminClient: AdminClient = createAdminClient
    val topics = List(topicName)
      .map(topic => new NewTopic(topic, 1, 1.toShort): NewTopic)
      .asJava

    adminClient.createTopics(topics).all().get()
    logger.info(s"Successfully created topic [$topicName]")
    adminClient.close()
  }

  private def createAdminClient: AdminClient = {
    val boostrapServers = kafka.getBootstrapServers
    val props           = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers)

    AdminClient.create(props)
  }

  private def makeBoostrapUrl: String = {
    val ipAddressViaToxiproxy = kafkaToxiProxyContainer.getHost
    val portViaToxiproxy      = kafkaToxiProxyContainer.getMappedPort(8666)
    s"PLAINTEXT://$ipAddressViaToxiproxy:$portViaToxiproxy"
  }

  def makeKafkaToxicHost: String = {
    val ipAddressViaToxiproxy = kafkaToxiProxyContainer.getHost
    val portViaToxiproxy      = kafkaToxiProxyContainer.getMappedPort(8666)
    s"$ipAddressViaToxiproxy:$portViaToxiproxy"
  }

  def makeSchemaServerHost: String = {
    s"http://localhost:${schemaRegistry.getMappedPort(SchemaServerPort)}"
  }

  override def beforeEach(): Unit = {
    if (!kafkaToxiProxy.isEnabled) {
      kafkaToxiProxy.enable()
    }
  }
  override def afterAll(): Unit = {
    kafka.stop()
    schemaRegistry.stop()
    kafkaToxiProxyContainer.stop()
  }
}

object KafkaSupport {
  val breakUpstreamConnectionToxiName   = "CUT_CONNECTION_UPSTREAM"
  val breakDownstreamConnectionToxiName = "CUT_CONNECTION_DOWNSTREAM"
}
