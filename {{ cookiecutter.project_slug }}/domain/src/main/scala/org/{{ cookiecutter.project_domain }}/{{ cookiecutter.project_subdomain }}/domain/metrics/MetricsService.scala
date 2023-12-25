package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics

import cats.Applicative
import io.micrometer.core.instrument._

trait MetricsService[F[_]] {
  def incrementCounter(metricName: Metric, tags: Tags)(implicit f: Applicative[F]): F[Unit]
}

object MetricsService {
  def apply[F[_]: Applicative](meterRegistry: MeterRegistry): MetricsService[F] = new MetricsService[F] {

    override def incrementCounter(metric: Metric, tags: Tags)(implicit f: Applicative[F]): F[Unit] =
      f.pure(meterRegistry.counter(metric.name, tags).increment())
  }
}

sealed trait Metric { def name: String }
case object TestMetric extends Metric { val name = "test" }

case object ProducerRetryTriggeredMetric extends Metric { val name = "custom.kafka.producer.retry_triggered" }
case object ProducerPublishErrorMetric   extends Metric { val name = "custom.kafka.producer.publish_error"   }
case object DomainItemDaoFailureMetric   extends Metric { val name = "custom.db.domain_item_v1.failure"      }
case object DomainItemDaoGetMetric       extends Metric { val name = "custom.db.domain_item_v1.get"          }
case object DomainItemDaoInsertMetric    extends Metric { val name = "custom.db.domain_item_v1.insert"       }
case object DomainItemDaoUpdateMetric    extends Metric { val name = "custom.db.domain_item_v1.update"       }
