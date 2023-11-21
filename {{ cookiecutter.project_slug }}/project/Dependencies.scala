import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val overrides: List[Nothing] = List.empty

  object Circe {
    private val circeVersion       = "0.14.6"
    private val circeOpticsVersion = "0.15.0"

    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParse   = "io.circe" %% "circe-parser"  % circeVersion
    lazy val circeOptics  = "io.circe" %% "circe-optics"  % circeOpticsVersion
  }

  object Cats {
    lazy val cats       = "org.typelevel"    %% "cats-core"   % "2.9.0"
    lazy val catsEffect = "org.typelevel"    %% "cats-effect" % "3.5.0"
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
    lazy val http4sDsl        = "org.http4s" %% "http4s-dsl"                % http4sVersion
    lazy val http4sServer     = "org.http4s" %% "http4s-ember-server"       % http4sVersion
    lazy val http4sClient     = "org.http4s" %% "http4s-ember-client"       % http4sVersion
    lazy val http4sCirce      = "org.http4s" %% "http4s-circe"              % http4sVersion
    lazy val http4sDropwizard = "org.http4s" %% "http4s-dropwizard-metrics" % http4sDropWizardVersion

    private lazy val http4sVersion           = "0.23.18"
    private lazy val http4sDropWizardVersion = "0.23.11"

    val http4sAll = Seq(
      libraryDependencies ++= List(
        Http4s.http4sServer,
        Http4s.http4sClient,
        Http4s.http4sDsl,
        Http4s.http4sCirce,
        http4sDropwizard
      )
    )
  }

  object Fs2 {
    private lazy val version = "3.1.1"
    lazy val fs2Core         = "co.fs2" %% "fs2-core" % version
  }

  object Testing {
    lazy val testFramework = "org.scalatest" %% "scalatest"                      % "3.2.15" % "test"
    lazy val containerTest = "com.dimafeng"  %% "testcontainers-scala-scalatest" % "0.40.12"
  }

  object Netty {
    private lazy val version = "4.1.63.Final"
    val all                  = "io.netty" % "netty-all" % version
  }

  object Metrics {
    private lazy val dropWizardMetricsVersion = "4.2.17"

    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion
    val metricsJson = "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion
    val metricsJvm  = "io.dropwizard.metrics" % "metrics-jvm"  % dropWizardMetricsVersion
  }
}
