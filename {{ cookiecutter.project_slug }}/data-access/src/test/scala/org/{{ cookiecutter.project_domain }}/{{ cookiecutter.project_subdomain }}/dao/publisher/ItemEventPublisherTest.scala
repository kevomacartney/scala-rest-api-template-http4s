package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.publisher

import cats.Applicative
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream
import vulcan.Codec
import vulcan.generic._
import fs2.kafka.vulcan._
import fs2.kafka.{vulcan, _}
import io.micrometer.core.instrument.Tags
import org.foxi.account.ItemRequested
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.publisher.ItemEventPublisherTest._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event.ItemRequestedEvent
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.KafkaHelper
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.{KafkaSupport, KafkaTestContext}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.duration.DurationInt

class ItemEventPublisherTest extends AnyWordSpec with KafkaSupport with MockFactory with KafkaHelper {

  "ItemEventPublisher" when {
    "should publish event to kafka" in {
      implicit val mockedMetricsService: MetricsService[IO] = stub[MetricsService[IO]]
      val topicName: String                                 = UUID.randomUUID().toString

      withKafka(topicName) { implicit kafkaContext =>
        withItemEventPublisher(topicName) { itemEventPublisher =>
          val itemRequestedEvent = ItemRequestedEvent(id = UUID.randomUUID().toString, name = "kevo")

          Stream
            .eval(IO(itemRequestedEvent))
            .through(itemEventPublisher.publish)
            .timeout(5.seconds)
            .compile
            .drain
            .unsafeRunSync()

          val message = withItemRequestedEventDeserializer { implicit deserializer =>
            readLastNKafkaMessages[String, ItemRequested](1, topicName).head
          }

          message.key shouldBe itemRequestedEvent.id
          message.record.id shouldBe itemRequestedEvent.id
          message.record.name shouldBe itemRequestedEvent.name
          message.record.at shouldBe itemRequestedEvent.at
        }
      }
    }

    "should retry messages when publishing fails" in {
      val topicName: String                                 = UUID.randomUUID().toString
      implicit val mockedMetricsService: MetricsService[IO] = stub[MetricsService[IO]]

      (mockedMetricsService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
        .when(*, *, *)
        .returns(IO.unit)

      withKafka(topicName) { implicit kafkaContext =>
        withItemEventPublisher(topicName) { itemEventPublisher =>
          val itemRequestedEvent = ItemRequestedEvent(id = UUID.randomUUID().toString, name = "kevo")
          breakKafkaConnection()

          val continueStream = Stream
            .eval(IO.sleep(2.seconds))
            .evalMap(_ => IO(reEstablishKafkaConnection()))

          val producerStream = Stream
            .eval(IO(itemRequestedEvent))
            .through(itemEventPublisher.publish)

          producerStream
            .merge(continueStream)
            .timeout(10.seconds)
            .compile
            .drain
            .unsafeRunSync()

          val message = withItemRequestedEventDeserializer { implicit deserializer =>
            readLastNKafkaMessages[String, ItemRequested](1, topicName).head
          }

          message.key shouldBe itemRequestedEvent.id
          message.record.id shouldBe itemRequestedEvent.id
          message.record.name shouldBe itemRequestedEvent.name
          message.record.at shouldBe itemRequestedEvent.at

          val expectedRetryTags = Tags.of("kafka_topic", topicName)
          (mockedMetricsService
            .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
            .verify(ProducerRetryTriggeredMetric, expectedRetryTags, *)
            .once()

          val expectedProducerErrorTags = Tags.of("cause", "TimeoutException", "kafka_topic", topicName)
          (mockedMetricsService
            .incrementCounter(_: Metric, _: Tags)(_: Applicative[IO]))
            .verify(ProducerPublishErrorMetric, expectedProducerErrorTags, *)
            .atLeastTwice()
        }
      }
    }
  }
}

object ItemEventPublisherTest {
  def withItemEventPublisher[T](
      topicName: String
  )(f: ItemEventPublisher => T)(implicit metricsService: MetricsService[IO], kafkaContext: KafkaTestContext): T = {
    val avroSettings                 = AvroSettings { SchemaRegistryClientSettings[IO](kafkaContext.schemaServer) }
    implicit val codec               = Codec.derive[ItemRequested]
    implicit val eventAvroSerializer = avroSerializer[ItemRequested].forValue(avroSettings)

    val producerSettings =
      ProducerSettings[IO, String, ItemRequested]
        .withBootstrapServers(kafkaContext.boostrapServers)
        .withAcks(Acks.One)
        .withProperties(
          Map(
            "max.block.ms" -> "100" // this is to make the test fail faster
          )
        )

    KafkaProducer
      .resource(producerSettings)
      .use { producer =>
        val publisher = ItemEventPublisher(producer, topicName)
        IO(f(publisher))
      }
      .unsafeRunSync()
  }
}
