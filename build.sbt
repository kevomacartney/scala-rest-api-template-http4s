import Dependencies._

name := "rest-api"
version := "0.1"
scalaVersion := "2.13.0"
idePackagePrefix := Some("org.example.com")

lazy val commonSettings = Seq(
  organization := "com.aleph.retriever",
  scalacOptions += "-Ypartial-unification",
  resolvers += "Confluent" at "https://packages.confluent.io/maven/",
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full),
  libraryDependencies ++= List(
    Circe.circeCore,
    Circe.circeGeneric,
    Circe.circeParse,
    Cats.cats,
    Cats.catsEffect,
    Cats.catsBird,
    Cats.catsBirdEffect,
    Logging.scalaLogging,
    Logging.logBack,
    Logging.jclOverSL4J,
    Time.nScalaTime,
    Testing.testFramework
  ),
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val `domain` = (project in file("./domain"))
  .settings(commonSettings)
  .settings(
    name := "domain",
    libraryDependencies ++= List(
      Finagle.finagleCore,
      Finch.finchCore,
      Metrics.metricsCore
    )
  )

lazy val `app` = (project in file("./app"))
  .dependsOn(domain, `web`)
  .settings(commonSettings)
  .settings(
    name := "app",
    libraryDependencies ++= List(
      Config.pureConfig,
      Fs2.fs2Core,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )

lazy val `end-to-end` = (project in file("./end-to-end"))
  .dependsOn(`test-support`, `domain`, `app`)
  .settings(commonSettings)
  .settings(
    name := "end-to-end",
    libraryDependencies ++= List(
      Config.pureConfig,
      Circe.circeOptics,
      Fs2.fs2Core,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )

lazy val `test-support` = (project in file("./test-support"))
  .dependsOn(`domain`)
  .settings(commonSettings)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Finch.finchCirce,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )

lazy val `web` = (project in file("./web"))
  .dependsOn(`domain`)
  .settings(commonSettings)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Fs2.fs2Core,
      Finch.finchCirce,
      Metrics.metricsCore,
      Metrics.metricsJson,
      Metrics.metricsJvm
    )
  )
