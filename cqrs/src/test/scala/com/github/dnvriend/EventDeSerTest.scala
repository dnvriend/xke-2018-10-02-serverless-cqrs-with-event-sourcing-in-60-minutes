package com.github.dnvriend

import play.api.libs.json.Json

class EventDeSerTest extends TestSpec {
  it should "serialize PersonCreated" in {
    val event: Event = PersonCreated("1", "Dennis", 0L)
    Json.toJson(event).toString() shouldBe """{"id":"1","name":"Dennis","timestamp":0,"__type__":"PersonCreated"}"""
  }

  it should "serialize PersonRenamed" in {
    val event: Event = PersonRenamed("1", "Dennis", 0L)
    Json.toJson(event).toString() shouldBe """{"id":"1","name":"Dennis","timestamp":0,"__type__":"PersonRenamed"}"""
  }

  it should "deserialize all events" in {
    val events: List[Event] = Json.parse("""[{"id":"1","name":"Dennis","timestamp":0,"__type__":"PersonCreated"}, {"id":"1","name":"Dennis","timestamp":0,"__type__":"PersonRenamed"}]""").as[List[Event]]
    events shouldBe List(PersonCreated("1", "Dennis", 0L), PersonRenamed("1", "Dennis", 0L))
  }
}
