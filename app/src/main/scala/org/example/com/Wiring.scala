package org.example.com

import cats.effect._
import com.codahale.metrics.MetricRegistry
import cats.syntax.all._
import com.codahale.metrics.jvm._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.example.com.config.ApplicationConfig
import org.example.com.ops.OpsServer
import org.example.com.services.ApiService
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.server.middleware.Metrics
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory.getPlatformMBeanServer
import java.time.Clock
import scala.concurrent.ExecutionContext.global

object Wiring {
  def initialise(
      appConfig: ApplicationConfig
  ): Resource[IO, (Server, Server)] = {
    val logger = LoggerFactory.getLogger("Example-Rest-API")
    logger.info("Starting app")

    implicit val metricsRegistry: MetricRegistry = new MetricRegistry
    registerJvmMetrics(metricsRegistry)

    for {
      itemRepository <- ItemRepository()
      ops            <- new OpsServer().create(appConfig.opsServerConfig)
      api            <- createServer(itemRepository, appConfig)
    } yield (api, ops)

  }

  private def createServer(repository: ItemRepository, appConfig: ApplicationConfig)(
      implicit metricRegistry: MetricRegistry
  ): Resource[IO, Server] = {
    val httpService = new ApiService(repository)

    val server        = Router("/" -> httpService.helloWorldService)
    val meteredRoutes = Metrics[IO](Dropwizard(metricRegistry, "server"))(server)

    BlazeServerBuilder[IO](global)
      .bindHttp(appConfig.restConfig.port, "localhost")
      .withHttpApp(meteredRoutes.orNotFound)
      .resource
  }

  private def registerJvmMetrics(metricsRegistry: MetricRegistry): Unit = {
    metricsRegistry.register("jvm.memory", new MemoryUsageGaugeSet)
    metricsRegistry.register("jvm.threads", new ThreadStatesGaugeSet)
    metricsRegistry.register("jvm.gc", new GarbageCollectorMetricSet)
    metricsRegistry.register("jvm.bufferpools", new BufferPoolMetricSet(getPlatformMBeanServer))
    metricsRegistry.register("jvm.classloading", new ClassLoadingGaugeSet)
    metricsRegistry.register("jvm.filedescriptor", new FileDescriptorRatioGauge)

  }
}
