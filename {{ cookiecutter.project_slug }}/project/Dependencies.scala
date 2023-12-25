import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val overrides: Seq[ModuleID] = Seq(
    Fs2.fs2Core,
    Fs2.fs2Io,
    Http4s.http4sCore,
    Cats.cats,
    Cats.catsEffect,
    Kafka.`kafka-clients`
  )

  object Circe {
    private val circeVersion       = "0.14.6"
    private val circeOpticsVersion = "0.15.0"

    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParse   = "io.circe" %% "circe-parser"  % circeVersion
    lazy val circeOptics  = "io.circe" %% "circe-optics"  % circeOpticsVersion
  }

  object Cats {
    lazy val cats       = "org.typelevel"    %% "cats-core"   % "2.10.0"
    lazy val catsEffect = "org.typelevel"    %% "cats-effect" % "3.5.2"
    lazy val catsRetry  = "com.github.cb372" %% "cats-retry"  % "3.1.0"
  }

  object Logging {
    lazy val logBack      = "ch.qos.logback"             % "logback-classic" % "1.4.7"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.5"
    lazy val jclOverSL4J  = "org.slf4j"                  % "jcl-over-slf4j"  % "2.0.5"

  }

  object Time {
    lazy val nScalaTime = "com.github.nscala-time" %% "nscala-time" % "2.32.0"
  }

  object Config {
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.17.4"
  }

  object Http4s {
    lazy val http4sDsl    = "org.http4s" %% "http4s-dsl"          % http4sVersion
    lazy val http4sServer = "org.http4s" %% "http4s-ember-server" % http4sVersion
    lazy val http4sClient = "org.http4s" %% "http4s-ember-client" % http4sVersion
    lazy val http4sCirce  = "org.http4s" %% "http4s-circe"        % http4sVersion
    lazy val http4sCore   = "org.http4s" %% "http4s-core"         % http4sVersion

    private lazy val http4sVersion = "0.23.24"

    val http4sAll = Seq(
      libraryDependencies ++= List(
        Http4s.http4sServer,
        Http4s.http4sClient,
        Http4s.http4sDsl,
        Http4s.http4sCirce
      )
    )
  }

  object Fs2 {
    private lazy val fs2Version      = "3.9.3"
    private lazy val fs2KafkaVersion = "3.2.0"

    lazy val fs2Core  = "co.fs2"          %% "fs2-core"  % fs2Version
    lazy val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion
    lazy val fs2Io    = "co.fs2"          %% "fs2-io"    % fs2Version

  }

  object Avro {
    private lazy val vulcanAvroVersion    = "3.2.0"
    private lazy val vulcanGenericVersion = "1.9.0"
    private lazy val avro4sVersion        = "4.1.1"

    lazy val vulcanAvro    = "com.github.fd4s"     %% "fs2-kafka-vulcan" % vulcanAvroVersion
    lazy val vulcanGeneric = "com.github.fd4s"     %% "vulcan-generic"   % vulcanGenericVersion
    lazy val avro4s        = "com.sksamuel.avro4s" %% "avro4s-core"      % avro4sVersion

  }

  object Testing {
    lazy val testContainerVersion = "1.19.3"

    lazy val testFramework = "org.scalatest" %% "scalatest"                      % "3.2.15"  % Test
    lazy val testContainer = "com.dimafeng"  %% "testcontainers-scala-scalatest" % "0.40.12" % Test
    lazy val scalaMock     = "org.scalamock" %% "scalamock"                      % "5.2.0"   % Test
    lazy val `apache-avro` = "io.confluent"  % "kafka-avro-serializer"           % "5.3.0"   % Test

    lazy val `testContainer-toxiproxy` = "org.testcontainers" % "toxiproxy"  % testContainerVersion % Test
    lazy val `testContainer-kafka`     = "org.testcontainers" % "kafka"      % testContainerVersion % Test
    lazy val `testContainer-postgres`  = "org.testcontainers" % "postgresql" % testContainerVersion % Test
  }

  object Netty {
    private lazy val version = "4.1.63.Final"
    val all                  = "io.netty" % "netty-all" % version
  }

  object Metrics {
    private val micrometerVersion        = "1.10.5"
    private val meters4sVersion          = "2.0.0"
    private val prometheusMetricsVersion = "0.24.6"

    val microMeterCore       = "io.micrometer" % "micrometer-core"                % micrometerVersion
    val microMeterPrometheus = "io.micrometer" % "micrometer-registry-prometheus" % micrometerVersion
    val `prometheus-metrics` = "org.http4s"    %% "http4s-prometheus-metrics"     % prometheusMetricsVersion
  }

  object Data {
    private val doobieVersion = "1.0.0-RC5"
    val `doobie-core`         = "org.tpolecat" %% "doobie-core" % doobieVersion
    val `doobie-postgres`     = "org.tpolecat" %% "doobie-postgres" % doobieVersion
    val `doobie-specs2`       = "org.tpolecat" %% "doobie-specs2" % doobieVersion
    val `doobie-h2`           = "org.tpolecat" %% "doobie-h2" % doobieVersion
    val `doobie-hikari`       = "org.tpolecat" %% "doobie-hikari" % doobieVersion
    val `doobie-scalaTest`    = "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test
  }

  object Kafka {
    private val kafkaVersion = "7.5.1-ccs"
    val `kafka-clients`      = "org.apache.kafka" % "kafka-clients" % kafkaVersion
  }
}
