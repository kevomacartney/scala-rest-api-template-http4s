package org.example.com.httpClient

import com.codahale.metrics.{MetricRegistry, Gauge => MGauge}
import com.twitter.finagle.stats._

import java.util.concurrent.ConcurrentHashMap

object MetricsStatsReceiver {
  private val gauges = new ConcurrentHashMap[String, Gauge]()

  def apply(metricRegistry: MetricRegistry): MetricsStatsReceiver = {
    new MetricsStatsReceiver(metricRegistry, gauges, true)
  }
}

class MetricsStatsReceiver(metrics: MetricRegistry, gauges: ConcurrentHashMap[String, Gauge], enableMeters: Boolean)
    extends StatsReceiver   {

  private def format(names: Seq[String]) =
    names.mkString(".")

  case class CounterMetricCounter(name: String) extends Counter {

    private val counter = metrics.counter(name)

    override def incr(delta: Long): Unit =
      counter.inc(delta)
  }

  case class MeterMetricCounter(name: String) extends Counter {

    private val meter = metrics.meter(name)

    override def incr(delta: Long): Unit =
      meter.mark(delta)
  }

  case class MetricGauge(name: String)(f: => Float) extends Gauge {

    metrics.register(
      name,
      new MGauge[Float]() {
        override def getValue: Float = f
      }
    )

    override def remove(): Unit = {
      metrics.remove(name)
      ()
    }
  }

  case class MetricStat(name: String) extends Stat {
    private val histogram = metrics.histogram(name)

    override def add(value: Float): Unit =
      histogram.update(value.toLong)
  }

  override def counter(verbosity: Verbosity, names: String*): Counter = {

    val formatted = format(names)

    if (enableMeters) {
      MeterMetricCounter(formatted)
    } else {
      CounterMetricCounter(formatted)
    }
  }

  override def counter(schema: CounterSchema): Counter =
    counter(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)

  override def stat(verbosity: Verbosity, names: String*): Stat =
    MetricStat(format(names))

  override def stat(schema: HistogramSchema): Stat =
    stat(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)

  override def addGauge(verbosity: Verbosity, names: String*)(f: => Float): Gauge =
    gauges.computeIfAbsent(format(names), name => MetricGauge(name)(f))

  override def addGauge(schema: GaugeSchema)(f: => Float): Gauge =
    addGauge(schema.metricBuilder.verbosity, schema.metricBuilder.name: _*)(f)

  override def repr: AnyRef = this
}
