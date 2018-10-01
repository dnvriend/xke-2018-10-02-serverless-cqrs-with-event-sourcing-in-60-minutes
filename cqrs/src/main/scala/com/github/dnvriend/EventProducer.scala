package com.github.dnvriend

import com.github.dnvriend.kinesis.{KinesisProducer, KinesisRecord}
import com.github.dnvriend.lambda.SamContext
import play.api.libs.json.Json

case class EventProducer(ctx: SamContext) {
  def produce(events: Event*): Unit = {
    KinesisProducer(ctx).produce("import:event-data-segment:event-intake-stream",
      events.toList.map(event => KinesisRecord("1", (Json.toJson(event).toString() + "\n").getBytes())))
  }
}
