package com.github

import com.github.dnvriend.kinesis.{KinesisProducer, KinesisRecord}
import com.github.dnvriend.lambda.SamContext
import io.leonard.TraitFormat.traitFormat
import play.api.libs.json.{Format, JsValue, Json}

import scala.util.Random

package object dnvriend {
  object CreatePerson {
    implicit val personFormat: Format[CreatePerson] = Json.format
  }
  case class CreatePerson(name: String)

  object RenamePerson {
    implicit val personFormat: Format[RenamePerson] = Json.format
  }
  case class RenamePerson(name: String)

  sealed trait Event
  object Event {
    implicit val eventFormat: Format[Event] = traitFormat[Event]("__type__") << Json.format[PersonCreated] << Json.format[PersonRenamed] << Json.format[NoOperation]
  }
  case class PersonCreated(id: String, name: String, timestamp: Long) extends Event
  case class PersonRenamed(id: String, name: String, timestamp: Long) extends Event
  case class NoOperation(id: String, name: String, timestamp: Long) extends Event

  object Person {
    implicit val format: Format[Person] = Json.format
  }
  case class Person(id: String, name: String, lastUpdated: Long)

  def randomNames(): List[String] = {
    scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("names.txt")).getLines().toList
  }

  def nextName(xs: List[String]): String = {
    xs(Random.nextInt(xs.length))
  }

  def appendNewLineToJson(msg: JsValue): String = {
    Json.toJson(msg).toString() + "\n"
  }

  def produce(ctx: SamContext)(xs: JsValue*): Unit = {
    val records: List[KinesisRecord] = xs.toList.map(appendNewLineToJson).map(_.getBytes).map(KinesisRecord("1", _))
    KinesisProducer(ctx).produce("import:event-data-segment:event-intake-stream", records)
  }
}
