<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/Wiring.scala
package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}
========
package org.{{ cookiecutter.project_subdomain }}.com
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/Wiring.scala

import cats.effect._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm._
import com.comcast.ip4s._
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/Wiring.scala
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.config.ApplicationConfig
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.dao.ItemRepository
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.ops.OpsHttpAdapter
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.web.RestService
========
import org.{{ cookiecutter.project_subdomain }}.com.config.ApplicationConfig
import org.{{ cookiecutter.project_subdomain }}.com.ops.OpsHttpAdapter
import org.{{ cookiecutter.project_subdomain }}.com.services.ApiService
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/Wiring.scala
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.server.middleware.Metrics
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory.getPlatformMBeanServer
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/Wiring.scala
import scala.concurrent.duration.DurationInt
========
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/Wiring.scala

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
      ops            <- new OpsHttpAdapter().create(appConfig.opsServerConfig)
      api            <- createServer(itemRepository, appConfig)
    } yield (api, ops)

  }

  private def createServer(repository: ItemRepository, appConfig: ApplicationConfig)(
    implicit metricRegistry: MetricRegistry
  ): Resource[IO, Server] = {
    val httpService = new RestService(repository)

    val server        = Router("/" -> httpService.helloWorldService)
    val meteredRoutes = Metrics[IO](Dropwizard(metricRegistry, "server"))(server)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(appConfig.restConfig.port).get)
      .withHttpApp(meteredRoutes.orNotFound)
<<<<<<<< HEAD:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_domain }}/{{ cookiecutter.project_subdomain }}/Wiring.scala
      .withShutdownTimeout(0.seconds)
========
>>>>>>>> main:{{ cookiecutter.project_slug }}/app/src/main/scala/org/{{ cookiecutter.project_subdomain }}/com/Wiring.scala
      .build
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
