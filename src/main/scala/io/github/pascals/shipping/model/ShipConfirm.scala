package io.github.pascals.shipping.model

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import vulcan.Codec
import vulcan.generic._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.Decoder.Result

case class ShipConfirm(
    `type`: String,
    order_id: String,
    delivery_id: String,
    tracking_number: String,
    carrier: String,
    fees: String,
    currency: String,
    country: String,
    timestamp: java.time.OffsetDateTime
)

object ShipConfirm {

  implicit val offsetDateTimeJsonCodec
      : Encoder[OffsetDateTime] with Decoder[OffsetDateTime] =
    new Encoder[OffsetDateTime] with Decoder[OffsetDateTime] {
      override def apply(a: OffsetDateTime): Json =
        Encoder.encodeString.apply(
          a.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )

      override def apply(c: HCursor): Result[OffsetDateTime] =
        Decoder.decodeString
          .map(s =>
            OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          )
          .apply(c)
    }
  implicit val shipConfirmJsonDeCodec: Decoder[ShipConfirm] =
    deriveDecoder[ShipConfirm]
  implicit val shipConfirmJsonEeCodec: Encoder[ShipConfirm] =
    deriveEncoder[ShipConfirm]
  implicit val offsetDateTimeAvroCodec: Codec[OffsetDateTime] =
    Codec[String].imap(OffsetDateTime.parse(_))(_.toString)
  implicit val shipConfirmAvroCodec: Codec[ShipConfirm] =
    Codec.derive[ShipConfirm]
}
