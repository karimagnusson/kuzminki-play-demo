package controllers

import java.time._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http.HttpEntity
import akka.util.ByteString
import scala.concurrent.ExecutionContext
import models.dvdrental._
import kuzminki.module.KuzminkiPlay
import kuzminki.api._
import kuzminki.fn._


@Singleton
class SelectController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val actor = Model.get[Actor]
  val film = Model.get[Film]
  val customer = Model.get[Customer]

  // /actor/result1

  /*
    SELECT
      "actor_id",
      "first_name",
      "last_name",
      "last_update"
    FROM "actor"
    WHERE "first_name"
    LIKE concat(?, '%')
    ORDER BY "actor_id" ASC
    LIMIT ?
  */

  def actorResult1 = Action.async {
    db
      .select(actor)
      .cols4(t => (
        t.actorId,
        t.firstName,
        t.lastName,
        t.lastUpdate
      ))
      .whereOne(_.firstName.startsWith("A"))
      .orderByOne(_.actorId.asc)
      .limit(10)
      //.sql(println)
      .run()
      .map { rows =>  
        val actors = rows.map {
          case (actorId, firstName, lastName, lastUpdate) =>
            Json.obj(
              "actor_id" -> actorId,
              "first_name" -> firstName,
              "last_name" -> lastName,
              "last_update" -> lastUpdate
            )
        }
        Ok(Json.toJson(actors))
      }
  }

  // /actor/result2

  /*
    SELECT
      "actor_id",
      "first_name",
      "last_name",
      "last_update"
    FROM "actor"
    ORDER BY "actor_id" ASC
    LIMIT ?
  */

  def actorResult2 = Action.async {
    db
      .select(actor)
      .colsRead(_.info)
      .all
      .orderByOne(_.actorId.asc)
      .limit(10)
      .run()
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /actor/result3

  /*
    SELECT
      "actor_id",
      "first_name",
      "last_name"
    FROM "actor"
    WHERE "last_name" LIKE concat(?, '%')
    ORDER BY "actor_id" ASC
    LIMIT ?
  */

  private val actorToJson: Tuple3[Int, String, String] => JsValue = {
    case (id, first, last) =>
      Json.obj("actor_id" -> id, "first_name" -> first, "last_name" -> last)
  }

  def actorResult3 = Action.async {
    db
      .select(actor)
      .cols3(_.idAndName)
      .whereOne(_.lastName.startsWith("B"))
      .orderByOne(_.actorId.asc)
      .limit(10)
      .runAs(actorToJson)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /actor/result4

  /*
    SELECT
      "actor_id",
      "first_name",
      "last_name"
    FROM "actor"
    WHERE "actor_id" > ?
    ORDER BY "actor_id" ASC
    LIMIT ?
  */

  private case class ActorFullName(id: Int, name: String) {
    def toJson = Json.obj("id" -> id, "name" -> name)
  }

  private implicit val toFullName: Tuple3[Int, String, String] => ActorFullName = {
    case (id, first, last) =>
      ActorFullName(id, first + " " + last)
  }

  def actorResult4 = Action.async {
    db
      .select(actor)
      .cols3(_.idAndName)
      .whereOne(_.actorId > 10)
      .orderByOne(_.actorId.asc)
      .limit(10)
      .runAs[ActorFullName]
      .map(actors => Ok(Json.toJson(actors.map(_.toJson))))
  }

  // /actor/result5/:actorId

  /*
    SELECT
      "actor_id",
      concat("first_name", ?, "last_name")
    FROM "actor"
    WHERE "actor_id" = ?
  */

  def actorResult5(actorId: Int) = Action.async {
    db
      .select(actor)
      .cols2(t => (
        t.actorId,
        Fn.concatWs(" ", t.firstName, t.lastName)
      ))
      .whereOne(_.actorId === actorId)
      .headOpt()
      .map {
        case Some(actor) =>
          Ok(Json.obj("id" -> actor._1, "name" -> actor._2))
        case None =>
          Ok(Json.obj("message" -> "actor not found"))
      }
  }

  // /film/modified-cols/:filmId

  /*
    SELECT
      "film_id",
      "title",
      upper("title"),
      substr("title", 4, 10),
      coalesce("description", ?),
      coalesce("length", ?),
      "release_year"
    FROM "film"
    WHERE "film_id" = ?
  */

  private val filmModifiedStm = db
    .select(film)
    .cols7(t => (
      t.filmId,
      t.title,
      Fn.upper(t.title),
      Fn.substr(t.title, 4, 10),
      Fn.coalesce(t.description, "No title"),
      Fn.coalesce(t.length, 0.toShort),
      t.releaseYear.asOpt
    ))
    .all
    .cacheWhere1(_.filmId.cacheEq)

  private val filmModifiedResult: Tuple7[Int, String, String, String, String, Short, Option[Short]] => JsValue = {
    case (id, title, titleUpper, titleSubstr, desc, length, year) =>
      Json.obj(
        "id" -> id,
        "title" -> title,
        "title_upper" -> titleUpper,
        "title_substr" -> titleSubstr,
        "desc" -> desc,
        "length" -> length,
        "year" -> year
      )
  } 

  def filmModifiedCols(filmId: Int) = Action.async {
    filmModifiedStm
      .headOptAs(filmId)(filmModifiedResult)
      .map {
        case Some(row) => Ok(row)
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }

  // /film/search-option

  /*
    SELECT
      "film_id",
      "title",
      "description",
      "release_year"
    FROM "film"
    WHERE "description" LIKE concat('%', ?, '%')
    AND "release_year" = ?
    ORDER BY "title"
    ASC LIMIT ?
  */

  private val filmToJson: Tuple4[Int, String, String, Short] => JsValue = {
    case (id, title, description, year) =>
      Json.obj(
        "id" -> id,
        "title" -> title,
        "description" -> description,
        "year" -> year
      )
  }

  def filmSearchOption = Action.async { request =>

    val params = request.queryString.map(p => p._1 -> p._2.mkString(","))

    db
      .select(film)
      .cols4(t => (
        t.filmId,
        t.title,
        t.description,
        t.releaseYear
      ))
      .whereOpts(t => Seq(
        t.title.startsWith(params.get("title")),
        t.description.like(params.get("description")),
        t.releaseYear === params.get("year").map(_.toShort)
      ))
      .orderByOne(_.title.asc)
      .limit(10)
      .run()
      .map(rows => Ok(Json.toJson(rows.map(filmToJson))))
  }

  // /film/search-or/:search

  /*
    SELECT
      "film_id",
      "title",
      "description",
      "release_year"
    FROM "film"
    WHERE ("title" ~* ? OR "description" ~* ?)
    ORDER BY "film_id" ASC
    LIMIT ?
  */

  def filmSearchOr(text: String) = Action.async {
    db
      .select(film)
      .cols4(t => (
        t.filmId,
        t.title,
        t.description,
        t.releaseYear
      ))
      .whereOne(t => Or(
        t.title ~* text,
        t.description ~* text
      ))
      .orderByOne(_.filmId.asc)
      .limit(10)
      .run()
      .map(rows => Ok(Json.toJson(rows.map(filmToJson))))
  }

  // /film/offset

  /*
    SELECT
      "film_id",
      "title",
      "release_year"
    FROM "film"
    WHERE "release_year" IS NOT NULL
    AND "release_year" > ?
    ORDER BY "release_year" ASC
    OFFSET ?
    LIMIT ?
  */

  val filmOffsetStm = db
    .select(film)
    .cols3(t => (
      t.filmId,
      t.title,
      t.releaseYear
    ))
    .whereOne(_.releaseYear.isNotNull)
    .orderByOne(_.releaseYear.asc)
    .limit(10)
    .cacheWhereWithOffset1(_.releaseYear.cacheGt)

  val filmOffsetResult: Tuple3[Int, String, Short] => JsValue = {
    case (id, title, year) =>
      Json.obj("id" -> id, "title" -> title, "year" -> year)
  }

  def filmOffset(minYear: Int, offset: Int) = Action.async {
    filmOffsetStm
      .runAs(minYear.toShort, offset)(filmOffsetResult)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /film/stream

  /*
    SELECT
      "film_id",
      "title",
      "release_year",
      "length"
    FROM "film"
    WHERE "release_year" IS NOT NULL
    AND "length" IS NOT NULL
  */

  val filmStreamStm = db
    .select(film)
    .cols4(t => (
      t.filmId,
      t.title,
      t.releaseYear,
      t.length
    ))
    .where(t => Seq(
      t.releaseYear.isNotNull,
      t.length.isNotNull
    ))
    .cache

  val filmToCsvLine: Tuple4[Int, String, Short, Short] => ByteString = {
    case (filmId, titleRaw, year, length) =>
      val title = titleRaw.replace(",", " ")
      ByteString(s"$filmId,$title,$year,$length\n")
  }

  def streamFilms = Action {
    val source = filmStreamStm.source.map(filmToCsvLine)

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, None, Some("text/csv"))
    )
  }
}




















