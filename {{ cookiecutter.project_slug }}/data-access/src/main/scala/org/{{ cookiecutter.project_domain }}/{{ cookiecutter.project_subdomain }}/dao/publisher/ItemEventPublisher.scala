package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.publisher

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2.Pipe
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import io.micrometer.core.instrument.Tags
import org.{{ cookiecutter.project_domain }}.account.ItemRequested
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.publisher.ItemEventPublisher.retryingOnErrors
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.KafkaError.KafkaProduceError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.event.ItemRequestedEvent
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.{ProducerRetryTriggeredMetric, MetricsService}
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.publisher.EventPublisher
import retry.{RetryDetails, RetryPolicies, retryingOnAllErrors}
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration.DurationInt

class ItemEventPublisher(
    producer: KafkaProducer.Metrics[IO, String, ItemRequested],
    topic: String
)(implicit metricsService: MetricsService[IO])
    extends EventPublisher[IO, ItemRequestedEvent]
    with LazyLogging {

  private def retryPolicy[A]: IO[A] => IO[A] = retryingOnErrors[A](topic)

  override def publish: Pipe[IO, ItemRequestedEvent, Unit] = _.evalMap { event =>
    for {
      record <- IO(ProducerRecord(topic, event.id, ItemRequestedEvent.mapToAvroRecord(event)))
      _      <- produceMessageWithErrorHandling(record)
      _      <- IO(logger.info(s"Produced kafka event: [$event"))
    } yield ()
  }

  private def produceMessageWithErrorHandling(record: ProducerRecord[String, ItemRequested]): IO[Unit] = {
    val produceRecordIo = producer
      .produce(ProducerRecords.one(record))
      .flatten

    retryPolicy(produceRecordIo).start
      .map(_ => IO.unit)
  }
}

object ItemEventPublisher {
  def apply(
      producer: KafkaProducer.Metrics[IO, String, ItemRequested],
      topic: String
  )(implicit metricsService: MetricsService[IO]): ItemEventPublisher =
    new ItemEventPublisher(producer, topic)

  private def retryingOnErrors[A](topic: String)(implicit metricsService: MetricsService[IO]): IO[A] => IO[A] = {
    val backoffRetryConfig = RetryPolicies.fibonacciBackoff[IO](50.milliseconds)

    retryingOnAllErrors(policy = backoffRetryConfig, onError = handleRetryOnError(topic))(_)
  }

  private def handleRetryOnError(topic: String)(e: Throwable, details: RetryDetails)(
      implicit metricsService: MetricsService[IO]
  ): IO[Unit] = {
    val upcomingDelay = details.upcomingDelay.map(t => s"${t.toMillis}ms")
    val error = KafkaProduceError(
      e,
      s"Could not produce message current-retries=${details.retriesSoFar}, next retry in: $upcomingDelay",
      topic
    )

    for {
      _ <- ErrorHandler.handle[IO](error)

      tags = Tags.of("kafka_topic", topic)
      _ <- if (details.retriesSoFar == 0)
            metricsService.incrementCounter(ProducerRetryTriggeredMetric, tags)
          else IO.unit
    } yield ()
  }
}
