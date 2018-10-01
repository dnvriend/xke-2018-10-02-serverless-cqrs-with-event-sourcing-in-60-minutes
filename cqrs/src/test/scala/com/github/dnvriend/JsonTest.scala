package com.github.dnvriend

import play.api.libs.json.Json

class JsonTest extends TestSpec {
  it should "append newline to json string" in {
    val event: Event = PersonCreated("1", "Dennis", 0)
    val str = appendNewLineToJson(Json.toJson(event))
    str should endWith("\n")
  }
}
