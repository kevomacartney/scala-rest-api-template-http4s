import sbt._

object Dependencies {

  object Circe {
    private val version = "0.13.0"

    lazy val circeCore    = "io.circe" %% "circe-core"    % version
    lazy val circeGeneric = "io.circe" %% "circe-generic" % version
    lazy val circeParse   = "io.circe" %% "circe-parser"  % version
    lazy val circeOptics  = "io.circe" %% "circe-optics"  % version
  }

  object Cats {
    lazy val cats           = "org.typelevel"    %% "cats-core"       % "2.4.2"
    lazy val catsEffect     = "org.typelevel"    %% "cats-effect"     % "2.5.0"
    lazy val catsBird       = "io.catbird"       %% "catbird-finagle" % "21.2.0"
    lazy val catsBirdEffect = "io.catbird"       %% "catbird-effect"  % "21.2.0"
    lazy val catsRetry      = "com.github.cb372" %% "cats-retry"      % "2.1.0"
  }

  object Logging {
    lazy val logBack      = "ch.qos.logback"             % "logback-classic" % "1.2.3"
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
    lazy val jclOverSL4J  = "org.slf4j"                  % "jcl-over-slf4j"  % "1.7.31"

  }

  object Time {
    lazy val nScalaTime = "com.github.nscala-time" %% "nscala-time" % "2.24.0"
  }

  object Config {
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % "0.13.0"
  }

  object Finagle {
    private lazy val finagleVersion = "21.2.0"
    lazy val finagleHttp            = "com.twitter" %% "finagle-http" % finagleVersion
    lazy val finagleCore            = "com.twitter" %% "finagle-core" % finagleVersion
  }

  object Finch {
    private lazy val finchVersion = "0.32.1"

    lazy val finchCore    = "com.github.finagle" %% "finchx-core"   % finchVersion
    lazy val finchCirce   = "com.github.finagle" %% "finchx-circe"  % finchVersion
    lazy val finchGeneric = "io.circe"           %% "circe-generic" % "0.9.0"
  }

  object Fs2 {
    private lazy val version = "2.5.0"
    lazy val fs2Core         = "co.fs2" %% "fs2-core" % version
  }

  object Testing {
    lazy val testFramework = "org.scalatest" %% "scalatest"                      % "3.2.0" % "test"
    lazy val containerTest = "com.dimafeng"  %% "testcontainers-scala-scalatest" % "0.38.8"
  }

  object Metrics {
    private lazy val dropWizardMetricsVersion = "4.1.20"

    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion
    val metricsJson = "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion
    val metricsJvm  = "io.dropwizard.metrics" % "metrics-jvm"  % dropWizardMetricsVersion
  }

  object Netty {
    private lazy val version = "4.1.63.Final"
    val all                  = "io.netty" % "netty-all" % version
  }

  val overrides = List(
    "com.twitter"   %% "finagle-http" % "21.3.0",
    "com.twitter"   %% "finagle-core" % "21.3.0",
    "org.typelevel" %% "cats-effect"  % "2.5.0"
  )
}
