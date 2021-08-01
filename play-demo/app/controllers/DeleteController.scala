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
import models.dvdrental._
import kuzminki.api._
import kuzminki.module.KuzminkiPlay


@Singleton
class DeleteController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val staff = Model.get[Staff]
  val film = Model.get[Film]

  val root = Paths.get("").toAbsolutePath.toString

  // /delete/inactive-staff

  /*
    DELETE FROM "staff"
    WHERE "active" = ?
    RETURNING
      "staff_id",
      "email"
  */

  val deleteResult: Tuple2[Int, String] => JsValue = {
    case (id, email) =>
      Json.obj("id" -> id, "email" -> email)
  }

  def deleteInactiveStaff = Action.async {
    db
      .delete(staff)
      .whereOne(_.isActive === false)
      .returning2(t => (
        t.staffId,
        t.email
      ))
      .runAs(deleteResult)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /staff/update-from-file

  // DELETE FROM "staff" WHERE "email" = ?
  
  val deleteFromFileStm = db
    .delete(staff)
    .cacheWhere1(_.email.cacheEq)

  def deleteFromFile = Action.async(parse.json) { request =>

    val fileName = (request.body \ "file_name").as[String]
    val path = Paths.get(root, "csv", fileName)

    val source = FileIO
      .fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), 256, true))
      .map(_.utf8String)

    deleteFromFileStm
      .fromSource(source)
      .map(_ => Ok(Json.obj("success" -> true)))
  }
}

















