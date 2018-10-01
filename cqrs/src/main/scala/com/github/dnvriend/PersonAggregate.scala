package com.github.dnvriend

import com.github.dnvriend.lambda.SamContext
import com.github.dnvriend.service.Aggregate
import play.api.libs.json.{Format, Json}
import scalaz._

object PersonAggregate {
  def aggregate(id: String, ctx: SamContext): Aggregate[Event, PersonAggregate, Unit] = {
    Aggregate[Event, PersonAggregate, Unit](id, "person_events", ctx, 100) { event =>
      State {
        case None => event match {
          case PersonCreated(_, name, timestamp) => (Option(PersonAggregate(id, name, timestamp)), ())
        }
        case Some(person) => event match {
          case PersonRenamed(_, newName, timestamp) => (Option(person.copy(name = newName).copy(lastUpdated = timestamp)), ())
        }
      }
    }
  }

  implicit val format: Format[PersonAggregate] = Json.format
}

case class PersonAggregate(id: String, name: String, lastUpdated: Long)