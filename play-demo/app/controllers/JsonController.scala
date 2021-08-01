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
import models.modeltypes._
import kuzminki.module.KuzminkiPlay
import kuzminki.api._
import kuzminki.fn._
import kuzminki.playjson.implicits._


@Singleton
class JsonController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val film = Model.get[Film]
  val actor = Model.get[Actor]

  // /json/film

  /*
    SELECT
      "film_id",
      "title",
      "description",
      "release_year",
      "length"
    FROM "film"
    WHERE "release_year" > ?
    ORDER BY "title" ASC
    LIMIT ?
  */
  
  def jsonFilms(year: Int) = Action.async {
    db
      .select(film)
      .colsJson(t => Seq(
        t.filmId,
        t.title,
        t.description,
        t.releaseYear,
        t.length
      ))
      .whereOne(_.releaseYear > year.toShort)
      .orderByOne(_.title.asc)
      .limit(10)
      .run()
      .map(rows => Ok(JsArray(rows)))
  }
  
  // /json/films/:filmId

  /*
    SELECT
      "film_id",
      upper("title"),
      replace("description", 'a', 'A'),
      "release_year"
    FROM "film"
    WHERE
      "film_id" = ?
  */
  
  val jsonFilmStm = db
      .select(film)
      .colsJson(t => Seq(
        ("id" -> t.filmId),
        ("title_upper" -> Fn.upper(t.title)),
        ("desc" -> Fn.replace(t.description, "a", "A")),
        ("year" -> t.releaseYear)
      ))
      .all
      .cacheWhere1(_.filmId.cacheEq)
  
  
  def jsonFilm(filmId: Int) = Action.async {
    jsonFilmStm
      .headOpt(filmId)
      .map {
        case Some(row) => Ok(row)
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }
  
  // /json/insert

  /*
    INSERT INTO "actor"
    ("first_name", "last_name")
    VALUES (?, ?)
    RETURNING
      "actor_id",
      "first_name",
      "last_name",
      "last_update"
  */
  
  def jsonInsert = Action.async(parse.json) { request =>

    val firstName = (request.body \ "first_name").as[String]
    val lastName = (request.body \ "last_name").as[String]

    db
      .insert(actor)
      .cols2(t => (
        t.firstName,
        t.lastName
      ))
      .returningJson(t => Seq(
        t.actorId,
        t.firstName,
        t.lastName,
        t.lastUpdate
      ))
      .run((firstName, lastName))
      .map(row => Ok(row))
  }
}




