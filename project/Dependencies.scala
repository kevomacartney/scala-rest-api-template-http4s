import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {

  val overrides = List(
    "org.typelevel" %% "cats-effect" % "2.5.0"
  )

  object Circe {
    private val version = "0.14.1"

    lazy val circeCore    = "io.circe" %% "circe-core"    % version
    lazy val circeGeneric = "io.circe" %% "circe-generic" % version
    lazy val circeParse   = "io.circe" %% "circe-parser"  % version
    lazy val circeOptics  = "io.circe" %% "circe-optics"  % version
  }

  object Cats {
    lazy val cats           = "org.typelevel"    %% "cats-core"       % "2.6.1"
    lazy val catsEffect     = "org.typelevel"    %% "cats-effect"     % "3.2.2"
    lazy val catsRetry      = "com.github.cb372" %% "cats-retry"      % "3.0.0"
  }

  object Logging {
    lazy val logBack      = "ch.qos.logback"             % "logback-classic" % "1.2.5"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.4"
    lazy val jclOverSL4J  = "org.slf4j"                  % "jcl-over-slf4j"  % "1.7.32"

  }

  object Time {
    lazy val nScalaTime = "com.github.nscala-time" %% "nscala-time" % "2.28.0"
  }

  object Config {
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.16.0"
  }

  object Http4s {
    lazy val http4sDsl             = "org.http4s" %% "http4s-dsl" % http4sVersion
    lazy val http4sServer          = "org.http4s" %% "http4s-blaze-server" % http4sVersion
    lazy val http4sClient          = "org.http4s" %% "http4s-blaze-client" % http4sVersion
    lazy val http4sCirce           = "org.http4s" %% "http4s-circe" % http4sVersion
    private lazy val http4sVersion = "0.23.1"
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
    private lazy val version = "3.1.0"
    lazy val fs2Core         = "co.fs2" %% "fs2-core" % version
  }

  object Testing {
    lazy val testFramework = "org.scalatest" %% "scalatest"                      % "3.2.9" % "test"
    lazy val containerTest = "com.dimafeng"  %% "testcontainers-scala-scalatest" % "0.39.6"
  }

  object Netty {
    private lazy val version = "4.1.63.Final"
    val all                  = "io.netty" % "netty-all" % version
  }

  object Metrics {
    private lazy val dropWizardMetricsVersion = "4.2.3"

    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion
    val metricsJson = "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion
    val metricsJvm  = "io.dropwizard.metrics" % "metrics-jvm"  % dropWizardMetricsVersion
  }
}
