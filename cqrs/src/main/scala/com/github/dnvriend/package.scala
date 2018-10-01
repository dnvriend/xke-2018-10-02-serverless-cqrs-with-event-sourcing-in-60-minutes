package com.github

import io.leonard.TraitFormat.traitFormat
import play.api.libs.json.{Format, Json}

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
    implicit val eventFormat: Format[Event] = traitFormat[Event]("__type__") << Json.format[PersonCreated] << Json.format[PersonRenamed]
  }
  case class PersonCreated(id: String, name: String, timestamp: Long) extends Event
  case class PersonRenamed(id: String, name: String, timestamp: Long) extends Event
}
