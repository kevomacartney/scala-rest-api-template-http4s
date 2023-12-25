package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics

import cats.Id
import io.micrometer.core.instrument.{MeterRegistry, Tags}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.domain.metrics.MetricsServiceTest.withMetricsService
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MetricsServiceTest extends AnyWordSpec with Matchers with ScalaFutures with EitherValues with BeforeAndAfterEach {

  "MetricsService" should {
    "record metrics" in {
      implicit val meterRegistry: MeterRegistry = new SimpleMeterRegistry()

      withMetricsService { metricService =>
        val tags = Tags.of("test", "test")
        metricService.incrementCounter(TestMetric, tags)

        meterRegistry.counter(TestMetric.name, tags).count() mustBe 1.0
      }
    }
  }
}

object MetricsServiceTest {
  def withMetricsService[A](testCode: MetricsService[Id] => A)(implicit meterRegistry: MeterRegistry): Unit = {
    val metricsService = MetricsService[Id](meterRegistry)
    testCode(metricsService)
  }
}
