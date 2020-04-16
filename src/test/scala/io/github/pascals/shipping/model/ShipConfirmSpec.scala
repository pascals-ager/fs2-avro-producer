package io.github.pascals.shipping.model

import cats.effect.IO
import fs2.kafka._
import vulcan.{
  AvroSettings,
  SchemaRegistryClientSettings,
  avroDeserializer,
  avroSerializer
}
import org.scalatest.funspec.AnyFunSpec
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.circe._
import io.circe.parser._

final class ShipConfirmSpec extends AnyFunSpec {

  import io.github.pascals.shipping.model.ShipConfirm._

  val schemaRegistryClient: MockSchemaRegistryClient =
    new MockSchemaRegistryClient()

  val schemaRegistryClientSettings: SchemaRegistryClientSettings[IO] =
    SchemaRegistryClientSettings[IO]("baseUrl")
      .withMaxCacheSize(100)
      .withCreateSchemaRegistryClient { (_, _, _) =>
        IO.pure(schemaRegistryClient)
      }

  val avroSettings: AvroSettings[IO] =
    AvroSettings(schemaRegistryClientSettings)

  val shipConfirmObj: ShipConfirm = ShipConfirm(
    "SHIP_CONFIRM",
    "orduid9874",
    "deluid9874",
    "987654312",
    "DHL",
    "5.32",
    "euros",
    "DE",
    java.time.OffsetDateTime.parse("2020-03-20T23:44:05.396+01:00")
  )
  val shipConfirmJson: Json = parse(
    """{
      |  "type": "SHIP_CONFIRM",
      |  "order_id": "orduid9875",
      |  "delivery_id": "deluid9875",
      |  "tracking_number": "987654312",
      |  "carrier": "DHL",
      |  "fees": "5.32",
      |  "currency": "euros",
      |  "country": "DE",
      |  "timestamp": "2020-03-20T23:44:05.396+01:00"
      |}""".stripMargin
  ).getOrElse(Json.Null)

  it("should be able to do avro de/serialization") {
    (for {
      serializer   <- avroSerializer[ShipConfirm].using(avroSettings).forValue
      serialized   <- serializer.serialize("topic", Headers.empty, shipConfirmObj)
      deserializer <- avroDeserializer[ShipConfirm].using(avroSettings).forValue
      deserialized <- deserializer.deserialize(
        "topic",
        Headers.empty,
        serialized
      )
    } yield assert(deserialized == shipConfirmObj)).unsafeRunSync
  }

  it("should be able to do json de/serialization") {
    (for {
      serialized <- IO(
        io.circe.Decoder[ShipConfirm].decodeJson(shipConfirmJson)
      )
      deserialized <- IO(
        io.circe
          .Encoder[ShipConfirm]
          .apply(serialized.getOrElse(shipConfirmObj))
      )
    } yield assert(deserialized == shipConfirmJson)).unsafeRunSync
  }

}
