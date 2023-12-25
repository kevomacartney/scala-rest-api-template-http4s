package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error

import cats.effect.IO
import cats.{Applicative, Id}
import io.micrometer.core.instrument.Tags
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.{
  DomainItemDaoFailureMetric,
  Metric,
  MetricsService,
  ProducerPublishErrorMetric
}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec

class ErrorHandlerTest extends AnyWordSpec with MockFactory {
  "ErrorHandler" should {
    "handle KafkaProduceError" in {
      implicit val metricService: MetricsService[Id] = mock[MetricsService[Id]]
      val expectedTags                               = Tags.of("kafka_topic", "topic", "cause", "Exception")

      (metricService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[Id]))
        .expects(ProducerPublishErrorMetric, expectedTags, *)
        .returns(())

      val error = KafkaError.KafkaProduceError(new Exception("test"), "error", "topic")
      ErrorHandler.handle(error)
    }
    "handle UnexpectedRepositoryError" in {
      implicit val metricService: MetricsService[Id] = mock[MetricsService[Id]]
      val expectedTags                               = Tags.of("cause", "Exception", "operation", "operation")

      (metricService
        .incrementCounter(_: Metric, _: Tags)(_: Applicative[Id]))
        .expects(DomainItemDaoFailureMetric, expectedTags, *)
        .returns(())

      val error = RepositoryError.UnexpectedRepositoryError(new Exception("test"), "There was an error", "operation")
      ErrorHandler.handle(error)
    }
  }
}

