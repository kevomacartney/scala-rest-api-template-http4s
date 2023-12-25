package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error

import cats.Applicative
import com.typesafe.scalalogging.LazyLogging
import io.micrometer.core.instrument._
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.KafkaError.KafkaProduceError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.error.RepositoryError.UnexpectedRepositoryError
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.{DomainItemDaoFailureMetric, ProducerPublishErrorMetric, MetricsService}

object ErrorHandler extends LazyLogging {
  def handle[F[_]](error: Error)(implicit metricsService: MetricsService[F], f: Applicative[F]): F[Unit] = error match {
    case kafkaError: KafkaError           => handleKafkaError(kafkaError)
    case repositoryError: RepositoryError => handleRepositoryError(repositoryError)
  }

  private def handleKafkaError[F[_]](
      error: KafkaError
  )(implicit metricsService: MetricsService[F], f: Applicative[F]): F[Unit] = error match {
    case KafkaProduceError(cause, error, topic) => handleKafkaProduceError(cause, error, topic)
  }

  private def handleRepositoryError[F[_]](repositoryError: RepositoryError)(
      implicit metricsService: MetricsService[F],
      f: Applicative[F]
  ): F[Unit] = {
    repositoryError match {
      case unexpectedRepositoryError: UnexpectedRepositoryError =>
        handleUnexpectedRepositoryError(unexpectedRepositoryError)
    }
  }

  private def handleUnexpectedRepositoryError[F[_]](
      error: UnexpectedRepositoryError
  )(implicit metricsService: MetricsService[F], f: Applicative[F]): F[Unit] = {
    val exceptionName = error.cause.getClass.getSimpleName
    val tags          = Tags.of("cause", exceptionName, "operation", error.operation)

    f.map(metricsService.incrementCounter(DomainItemDaoFailureMetric, tags))(_ =>
      logger.error(error.errorMessage, error.cause)
    )
  }
  private def handleKafkaProduceError[F[_]](cause: Throwable, error: String, topic: String)(
      implicit metricsService: MetricsService[F],
      f: Applicative[F]
  ): F[Unit] = {
    val exceptionName = cause.getClass.getSimpleName
    val tags          = Tags.of("kafka_topic", topic, "cause", exceptionName)

    f.map(metricsService.incrementCounter(ProducerPublishErrorMetric, tags))(_ => logger.error(error, cause))
  }

}
