import Dependencies._

name := "{{ cookiecutter.project_slug }}"
version := git.gitHeadCommit.value.getOrElse("unknown")
scalaVersion := "2.13.11"

enablePlugins(DockerPlugin, GitVersioning)

lazy val commonSettings = Seq(
  scalaVersion := "2.13.11",
  organization := "org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}",
  resolvers += "Confluent" at "https://packages.confluent.io/maven/",
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  libraryDependencies ++= List(
    Circe.circeCore,
    Circe.circeGeneric,
    Circe.circeParse,
    Cats.cats,
    Cats.catsEffect,
    Logging.scalaLogging,
    Logging.logBack,
    Logging.jclOverSL4J,
    Time.nScalaTime,
    Testing.testFramework,
    Testing.scalaMock
  ),
  dependencyOverrides ++= Dependencies.overrides,
  publish := {},
  //  allows for graceful shutdown of containers once the tests have finished running.
  Test / fork := true
)

lazy val assemblySettings = Seq(
  assembleArtifact := true,
  assembly / assemblyMergeStrategy := {
    case x if x.contains("module-info.class") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val `rest-service` = (project in file("."))
  .settings(assemblySettings)
  .enablePlugins(DockerPlugin)
  .settings(
    assembly / mainClass := Some("org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.Main"),
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    assembly / assemblyOutputPath := file(s"output/${(assembly / assemblyJarName).value}"),
    docker / dockerfile := {
      // The assembly task generates a fat JAR file
      val artifact: File     = assembly.value
      val artifactTargetPath = s"output/${(assembly / assemblyJarName).value}"

      new Dockerfile {
        from("openjdk:17-slim-buster")
        add(artifact, artifactTargetPath)
        entryPoint("java", "-jar", artifactTargetPath)
        expose(8080, 5002)
      }
    },
    docker / imageNames := Seq(
      ImageName(s"${name.value}:${version.value}")
    )
  )
  .aggregate(app, `avro-schema`, domain, `data-access`, `test-support`, `end-to-end`, web)
  .dependsOn(app, `avro-schema`, domain, `data-access`, `test-support`, `end-to-end`, web)

lazy val `app` = (project in file("./app"))
  .dependsOn(domain, `web`, `data-access`)
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(
    name := "app",
    libraryDependencies ++= List(
      Config.pureConfig,
      Fs2.fs2Core,
      Fs2.fs2Kafka,
      Data.`doobie-core`,
      Data.`doobie-hikari`,
      Data.`doobie-postgres`,
      Metrics.microMeterCore,
      Metrics.microMeterPrometheus,
      Metrics.`prometheus-metrics`
    )
  )

lazy val `avro-schema` = (project in file("./avro-schema"))
  .settings(assemblySettings)
  .settings(
    scalaVersion := "2.13.11",
    organization := "com.aleph.retriever",
    name := "avro-schema",
    Compile / sourceGenerators += (Compile / avroScalaGenerate).taskValue
  )

lazy val `domain` = (project in file("./domain"))
  .dependsOn(`avro-schema`)
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(
    name := "domain",
    libraryDependencies ++= List(
      Metrics.microMeterCore,
      Fs2.fs2Core
    )
  )

lazy val `data-access` = (project in file("./data-access"))
  .dependsOn(`domain`, `avro-schema`, `test-support` % "compile->compile;test->test")
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(
    name := "data-access",
    libraryDependencies ++= List(
      Metrics.microMeterCore,
      Fs2.fs2Kafka,
      Avro.avro4s,
      Avro.vulcanAvro,
      Avro.vulcanGeneric,
      Cats.catsRetry,
      Testing.testContainer,
      Data.`doobie-core`,
      Data.`doobie-postgres`,
      Data.`doobie-hikari`,
      Data.`doobie-scalaTest`
    )
  )

lazy val `end-to-end` = (project in file("./end-to-end"))
  .dependsOn(`domain`, `app`, `test-support` % "compile->compile;test->test")
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "end-to-end",
    libraryDependencies ++= List(
      Config.pureConfig,
      Circe.circeOptics,
      Fs2.fs2Core,
      Metrics.microMeterCore,
      Metrics.microMeterPrometheus,
      Testing.testContainer,
      Testing.`testContainer-kafka`,
      Testing.`testContainer-toxiproxy`,
      Testing.`testContainer-postgres`
    )
  )

lazy val `web` = (project in file("./web"))
  .dependsOn(`domain`, `test-support` % "compile->compile;test->test")
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Metrics.microMeterCore
    )
  )

lazy val `test-support` = (project in file("./test-support"))
  .dependsOn(`domain`)
  .settings(assemblySettings)
  .settings(commonSettings)
  .settings(Http4s.http4sAll)
  .settings(
    name := "test-support",
    libraryDependencies ++= List(
      Metrics.microMeterCore,
      Metrics.microMeterPrometheus,
      Fs2.fs2Kafka,
      Avro.avro4s,
      Avro.vulcanAvro,
      Avro.vulcanGeneric,
      Testing.testContainer,
      Testing.`testContainer-kafka`,
      Testing.`testContainer-toxiproxy`,
      Testing.`testContainer-postgres`,
      Testing.`apache-avro`,
      Data.`doobie-core`,
      Data.`doobie-postgres`,
      Data.`doobie-hikari`
    )
  )
