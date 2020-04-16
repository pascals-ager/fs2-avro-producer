package io.github.pascals.shipping.kafka

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import fs2.{Stream, text}
import fs2.kafka._
import io.circe.parser._
import io.github.pascals.shipping.model.ShipConfirm
import io.github.pascals.shipping.model.ShipConfirm._
import vulcan.{
  AvroSettings,
  SchemaRegistryClientSettings,
  avroDeserializer,
  avroSerializer
}

object AvroProducer extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

    val avroSettings: AvroSettings[IO] =
      AvroSettings {
        SchemaRegistryClientSettings[IO]("http://192.168.99.107:32081")
      }

    implicit val shipConfirmSerializer: RecordSerializer[IO, ShipConfirm] =
      avroSerializer[ShipConfirm].using(avroSettings)

    implicit val shipConfirmDeserializer: RecordDeserializer[IO, ShipConfirm] =
      avroDeserializer[ShipConfirm].using(avroSettings)

    val producerSettings: ProducerSettings[IO, Option[String], ShipConfirm] =
      ProducerSettings[IO, Option[String], ShipConfirm]
        .withBootstrapServers("192.168.99.107:32100")

    val stream: Stream[IO, ProducerResult[Option[String], ShipConfirm, Unit]] =
      producerStream[IO]
        .using(producerSettings)
        .flatMap { producer: KafkaProducer[IO, Option[String], ShipConfirm] =>
          val inner: Stream[IO, ProducerResult[Option[String], ShipConfirm, Unit]] = Stream.resource(Blocker[IO]).flatMap { blocker =>
            fs2.io
              .readInputStream(
                IO(getClass.getClassLoader.getResourceAsStream("ship_confirm.txt")),
                4096,
                blocker
              )
              .through(text.utf8Decode)
              .through(text.lines)
              .mapAsync(2)(rec => IO(decode[ShipConfirm](rec).toOption))
              .evalTap {
                case Some(_) => IO()
                case None    => IO(println())
              }
              .flattenOption
              .mapAsync(2) { rec =>
                IO(
                  ProducerRecords
                    .one(ProducerRecord("pascals-one-rep", None, rec))
                )
              }
              .through(produce(producerSettings, producer))
          }
          inner
        }

    stream.compile.drain.as(ExitCode.Success)
  }
}
