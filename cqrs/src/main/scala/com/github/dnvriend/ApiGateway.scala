package com.github.dnvriend

import java.util.UUID

import com.github.dnvriend.lambda._
import com.github.dnvriend.lambda.annotation.policy.{AmazonDynamoDBFullAccess, AmazonKinesisFullAccess, CloudWatchFullAccess}
import com.github.dnvriend.lambda.annotation.{HttpHandler, KinesisConf, ScheduleConf}
import play.api.libs.json.{JsValue, Json}
import scalaz.Scalaz._
import com.github.dnvriend.lambda.JsonReads.nothingReads
import com.github.dnvriend.repo.JsonRepository
import com.github.dnvriend.repo.dynamodb.DynamoDBJsonRepository
import scalaz.Validation

import scala.compat.Platform
import scala.util.Try

@HttpHandler(path = "/person", method = "put")
class CreatePersonHandler extends JsonApiGatewayHandler[CreatePerson] {
  override def handle(person: Option[CreatePerson],
                      pathParams: Map[String, String],
                      requestParams: Map[String, String],
                      request: HttpRequest,
                      ctx: SamContext): HttpResponse = {

    person.fold(HttpResponse.validationError.withBody(Json.toJson("Could not deserialize"))) { person =>
      val id = UUID.randomUUID().toString
      val now = Platform.currentTime
      val event = PersonCreated(id, person.name, now)

      PersonAggregate.aggregate(id, ctx).handle {
        case None => (PersonAggregate(id, person.name, now), event)
        case Some(aggregate) => (aggregate, NoOperation(id, aggregate.name, now))
      }

      produce(ctx)(Json.toJson(event))
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
    (data.toSuccessNel("Could not deserialize message") |@| pathParams.get("id").toSuccessNel("No id found in path"))((cmd, id) => (cmd, id, Platform.currentTime))
      .fold(err => HttpResponse.validationError.withBody(Json.toJson(err.toList)), { case (person, id, now) =>

        val event = PersonRenamed(id, person.name, now)

        PersonAggregate.aggregate(id, ctx).handle {
          case Some(aggregate) => (aggregate.copy(name = person.name, lastUpdated = now), event)
        }

        produce(ctx)(Json.toJson(event))
        HttpResponse(200, Json.toJson(event), Map.empty)
      })
  }
}

@HttpHandler(path = "/person/{id}", method = "get")
class ReadPerson extends JsonApiGatewayHandler[Nothing] {
  override def handle(person: Option[Nothing],
                      pathParams: Map[String, String],
                      requestParams: Map[String, String],
                      request: HttpRequest,
                      ctx: SamContext): HttpResponse = {

    pathParams.get("id")
      .fold(HttpResponse.notFound.withBody(Json.toJson("No id found in path")))(id => {
        PersonAggregate.aggregate(id, ctx)
          .currentState
          .fold(HttpResponse.notFound.withBody(Json.toJson("No person found for id: " + id)))(person => {
            HttpResponse.ok.withBody(Json.toJson(person))
          })
      })
  }
}

@HttpHandler(path = "/person", method = "get")
class ListPersons extends JsonDynamoRepoApiGatewayHandler[Person]("people_table") {
  override def handle(data: Option[Person],
                      pathParams: Map[String, String],
                      requestParams: Map[String, String],
                      request: HttpRequest,
                      ctx: SamContext, repo: JsonRepository): HttpResponse = {
    val people: List[Person] = repo.list[Person](100).map(_._2)
    HttpResponse.ok.withBody(Json.toJson(people))
  }
}

@ScheduleConf(schedule = "rate(1 minute)")
@AmazonKinesisFullAccess
@AmazonDynamoDBFullAccess
class EventGenerator extends ScheduledEventHandler {
  override def handle(event: ScheduledEvent, ctx: SamContext): Unit = {
    val names: List[String] = randomNames()
    val events: List[JsValue] = List.fill(100) {
      val id = UUID.randomUUID().toString
      val now = Platform.currentTime

      val personCreated = PersonCreated(id, nextName(names), now)
      PersonAggregate.aggregate(id, ctx).handle {
        case None => (PersonAggregate(id, personCreated.name, now), personCreated)
      }

      val personRenamed = PersonRenamed(id, nextName(names), now)
      PersonAggregate.aggregate(id, ctx).handle {
        case Some(aggregate) => (aggregate.copy(name = personRenamed.name, lastUpdated = now), personRenamed)
      }

      List(Json.toJson(personCreated), Json.toJson(personRenamed))
    }.flatten
    produce(ctx)(events:_*)
  }
}

@CloudWatchFullAccess
@AmazonKinesisFullAccess
@AmazonDynamoDBFullAccess
@KinesisConf(stream = "import:event-data-segment:event-intake-stream", startingPosition = "TRIM_HORIZON")
class ReadModelEventHandler extends KinesisEventHandler {
  override def handle(events: List[KinesisEvent], ctx: SamContext): Unit = {
    val repo = DynamoDBJsonRepository("people_table", ctx, "id", "json")
    val msg: String = Validation.fromTryCatchNonFatal {
      events.map(_.dataAs[Event]).foreach {
        case PersonCreated(id, name, timestamp) =>
          repo.put(id, Person(id, name, timestamp))
        case PersonRenamed(id, name, timestamp) =>
          repo.update(id, Person(id, name, timestamp))
      }
    }.bifoldMap(_.getMessage)( _ => s"Done writing ${events.length} events")
    println(msg)
  }
}