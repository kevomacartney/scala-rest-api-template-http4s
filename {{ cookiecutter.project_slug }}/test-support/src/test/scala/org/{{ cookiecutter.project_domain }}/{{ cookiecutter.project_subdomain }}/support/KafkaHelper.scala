package org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.kafka.ValueDeserializer
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings, avroDeserializer}
import org.foxi.account.ItemRequested
import org.{{ cookiecutter.project_domain }}.{{ cookiecutter.project_subdomain }}.support.kafka.KafkaTestContext
import vulcan.Codec
import vulcan.generic.MagnoliaCodec

trait KafkaHelper {

  def withItemRequestedEventDeserializer[T](
      f: ValueDeserializer[IO, ItemRequested] => T
  )(implicit kafkaContext: KafkaTestContext): T = {
    implicit val codec: Codec[ItemRequested] = Codec.derive[ItemRequested]
    val avroSettings = AvroSettings {
      SchemaRegistryClientSettings[IO](kafkaContext.schemaServer)
    }.withAutoRegisterSchemas(false)

    avroDeserializer[ItemRequested]
      .forValue(avroSettings)
      .use { deserializer =>
        IO(f(deserializer))
      }
      .unsafeRunSync()
  }
}
