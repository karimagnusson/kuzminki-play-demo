package controllers

import java.nio.file.{Path, Paths}
import java.time._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.http.HttpEntity
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.ExecutionContext
import kuzminki.module.KuzminkiPlay
import models.dvdrental._
import kuzminki.api._


@Singleton
class UpdateController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val staff = Model.get[Staff]
  val film = Model.get[Film]

  val root = Paths.get("").toAbsolutePath.toString

  // /update/staff1

  // UPDATE "staff" SET active = ? WHERE "staff_id" = ?

  def updateStaff1 = Action.async(parse.json) { request =>

    val staffId = (request.body \ "staff_id").as[Int]

    db
      .update(staff)
      .setOne(_.isActive ==> false)
      .whereOne(_.staffId === staffId)
      .run()
      .map(_ => Ok(Json.obj("success" -> true)))
  }

  // /update/staff2

  /*
    UPDATE "staff"
    SET email = ?
    WHERE "staff_id" = ?
    RETURNING
      "staff_id",
      "email"
  */

  def updateStaff2 = Action.async(parse.json) { request =>

    val staffId = (request.body \ "staff_id").as[Int]
    val email = (request.body \ "email").as[String]

    db
      .update(staff)
      .setOne(_.email ==> email)
      .whereOne(_.staffId === staffId)
      .returning2(t => (
        t.staffId,
        t.email
      ))
      .headOpt()
      .map {
        case Some(row) =>
          Ok(Json.obj("staff_id" -> row._1, "email" -> row._2))
        case None =>
          Ok(Json.obj("message" -> "row not updated"))
      }
  }

  // /update/staff3

  /*
    UPDATE "staff"
    SET email = ?,
        active = ?
    WHERE "staff_id" = ?
    RETURNING
      "staff_id",
      "username",
      "email",
      "store_id",
      "active"
  */

  def updateStaff3 = Action.async(parse.json) { request =>

    val staffId = (request.body \ "staff_id").as[Int]
    val email = (request.body \ "email").as[String]
    val isActive = (request.body \ "is_active").as[Boolean]

    db
      .update(staff)
      .set(t => Seq(
        t.email ==> email,
        t.isActive ==> isActive
      ))
      .whereOne(_.staffId === staffId)
      .returningRead(_.info)
      .headOpt()
      .map {
        case Some(row) =>
          Ok(Json.toJson(row))
        case None =>
          Ok(Json.obj("message" -> "row not updated"))
      }
  }

  // /update/film/increment/filmId

  /*
    UPDATE "film"
    SET release_year = release_year + ?,
        length = length - ?
    WHERE "film_id" = ?
    AND "release_year" IS NOT NULL
    AND "length" IS NOT NULL
    RETURNING
      "title",
      "release_year",
      "length"
  */

  def filmIncrement(filmId: Int) = Action.async {
    db
      .update(film)
      .set(t => Seq(
        t.releaseYear += 1,
        t.length -= 5
      ))
      .where(t => Seq(
        t.filmId === filmId,
        t.releaseYear.isNotNull,
        t.length.isNotNull
      ))
      .returning3(t => (
        t.title,
        t.releaseYear,
        t.length
      ))
      .headOpt()
      .map {
        case Some(row) =>
          Ok(Json.obj("title" -> row._1, "year" -> row._2, "length" -> row._3))
        case None =>
          Ok(Json.obj("message" -> "film not found"))
      }
  }

  // /update/staff/cache/

  // UPDATE "staff" SET "email" = ?, "active" = ? WHERE "email" = ?

  val staffUpdateCacheStm = db
    .update(staff)
    .cacheSet2(t => (
      t.email.cacheAssign,
      t.isActive.cacheAssign
    ))
    .cacheWhere1(_.email.cacheEq)

  def staffUpdateCache = Action.async(parse.json) { request =>

    val oldEmail = (request.body \ "old_email").as[String]
    val newEmail = (request.body \ "new_email").as[String]
    val isActive = (request.body \ "is_active").as[Boolean]

    staffUpdateCacheStm
      .runNum((newEmail, isActive), oldEmail)
      .map(num => Ok(Json.obj("updated" -> num)))
  }

  // /update/staff/from-file/:fileName

  // UPDATE "staff" SET "email" = ?, "active" = ? WHERE "email" = ?

  val toBool: String => Boolean = {
    case "t" => true
    case _ => false
  }

  val parseCsv: String => Tuple2[Tuple2[String, Boolean], String] = { line => 
    line.split(',') match {
      case Array(currentEmail, email, status) =>
        ((email, toBool(status)), currentEmail)
      case _ =>
        throw new Exception("invalid file")
    }
  }

  def updateFromFile = Action.async(parse.json) { request =>

    val fileName = (request.body \ "file_name").as[String]
    val path = Paths.get(root, "csv", fileName)

    val source = FileIO
      .fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), 256, true))
      .map(_.utf8String)
      .filter(_.size > 0)
      .map(parseCsv)

    staffUpdateCacheStm
      .fromSource(source)
      .map(_ => Ok(Json.obj("success" -> true)))
  }
}

















