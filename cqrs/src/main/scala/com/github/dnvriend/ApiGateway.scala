package com.github.dnvriend

import java.util.UUID

import com.github.dnvriend.lambda._
import com.github.dnvriend.lambda.annotation.policy.{AmazonKinesisFullAccess, CloudWatchFullAccess}
import com.github.dnvriend.lambda.annotation.{HttpHandler, KinesisConf, ScheduleConf}
import play.api.libs.json.Json
import scalaz.Scalaz._

import scala.compat.Platform

@HttpHandler(path = "/person", method = "put")
class CreatePersonHandler extends JsonApiGatewayHandler[CreatePerson] {
  override def handle(person: Option[CreatePerson],
                      pathParams: Map[String, String],
                      requestParams: Map[String, String],
                      request: HttpRequest,
                      ctx: SamContext): HttpResponse = {
    person.map { identity }.fold(HttpResponse.validationError.withBody(Json.toJson("Could not deserialize"))) { person =>
      val id = UUID.randomUUID().toString
      val timestamp = Platform.currentTime
      val event = PersonCreated(id, person.name, timestamp)
      EventProducer(ctx).produce(event)
      HttpResponse(200, Json.toJson(event), Map.empty)
    }
  }
}

@HttpHandler(path = "/person/{id}", method = "post")
class RenamePersonHandler extends JsonApiGatewayHandler[RenamePerson] {
  override def handle(data: Option[RenamePerson],
                      pathParams: Map[String, String],
                      requestParams: Map[String, String],
                      request: HttpRequest,
                      ctx: SamContext): HttpResponse = {
    (data.toSuccessNel("Could not deserialize message") |@| pathParams.get("id").toSuccessNel("No id in path"))((cmd, id) => (cmd, id, Platform.currentTime))
      .fold(err => HttpResponse.validationError.withBody(Json.toJson(err.toList)), { case (person, id, timestamp) =>
        HttpResponse(200, Json.toJson(PersonRenamed(id, person.name, timestamp)), Map.empty)
      })
  }
}

@ScheduleConf(schedule = "rate(1 minute)")
@AmazonKinesisFullAccess
class EventGenerator extends ScheduledEventHandler {
  override def handle(event: ScheduledEvent, ctx: SamContext): Unit = {
    val id = UUID.randomUUID().toString
    val timestamp = Platform.currentTime
    EventProducer(ctx).produce(
      PersonCreated(id, "Dennis", timestamp),
      PersonRenamed(id, "DennisRenamed", timestamp),
    )
  }
}

@CloudWatchFullAccess
@AmazonKinesisFullAccess
@KinesisConf(stream = "import:event-data-segment:event-intake-stream", startingPosition = "TRIM_HORIZON")
class ReadModelEventHandler extends KinesisEventHandler {
  override def handle(events: List[KinesisEvent], ctx: SamContext): Unit = {
    events.foreach { event =>
      println(event.dataAs[Event])
    }
  }
}
