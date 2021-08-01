package controllers

import java.nio.file.{Path, Paths}
import java.time._
import java.security.MessageDigest
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
import kuzminki.model._
import models.modeltypes._
import kuzminki.api._
import kuzminki.module.KuzminkiPlay


@Singleton
class InsertController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val staff = Model.get[Staff]
  val film = Model.get[Film]
  val actor = Model.get[Actor]
  val customer = Model.get[Customer]
  val store = Model.get[Store]

  val root = Paths.get("").toAbsolutePath.toString

  // /insert/actor1

  /*
    INSERT INTO "actor"
    ("first_name", "last_name")
    VALUES (?, ?)
    RETURNING "actor_id"
  */
  
  def insertActor1 = Action.async(parse.json) { request =>

    val firstName = (request.body \ "first_name").as[String]
    val lastName = (request.body \ "last_name").as[String]

    db
      .insert(actor)
      .cols2(t => (
        t.firstName,
        t.lastName
      ))
      .returning1(_.actorId)
      .sql(println)
      .run((firstName, lastName))
      .map(id => Ok(Json.obj("actor_id" -> id)))
  }

  // /insert/actor2

  // INSERT INTO "actor" ("first_name", "last_name") VALUES (?, ?)
  
  def insertActor2 = Action.async(parse.json) { request =>
    
    val firstName = (request.body \ "first_name").as[String]
    val lastName = (request.body \ "last_name").as[String]

    db
      .insert(actor)
      .data(t => Seq(
        t.firstName ==> firstName,
        t.lastName ==> lastName
      ))
      .run()
      .map(_ => Ok(Json.obj("success" -> true)))
  }

  // /insert/actor3

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
  
  def insertActor3 = Action.async(parse.json) { request =>
    
    val data = AddActor(
      (request.body \ "first_name").as[String],
      (request.body \ "last_name").as[String]
    )

    db
      .insert(actor)
      .colsWrite(_.addUser)
      .returningRead(_.info)
      .run(data)
      .map(row => Ok(Json.toJson(row)))
  }

  // actor list

  val insertActorStm = db
    .insert(actor)
    .cols2(t => (
      t.firstName,
      t.lastName
    ))
    .cache

  val actorList = List(
    ("Johnny", "Kim"),
    ("Oskar", "Sima"),
    ("Buck", "Deachman"),
    ("Joseph", "Crehan"),
    ("Bert", "Grund"),
    ("John", "Putch"),
    ("Kelly", "Aisenstat"),
    ("Jason", "Alderman"),
    ("Scott", "Rosenberg"),
    ("Beau", "Borders")
  )

  // /insert/actors/list

  // INSERT INTO "actor" ("first_name", "last_name") VALUES (?, ?), (?, ?), (?, ?) .....

  def insertActorsList = Action.async {
    insertActorStm
      .runListNum(actorList)
      .map(num => Ok(Json.obj("rows" -> num)))
  }

  // /insert/actors/stream-list

   // INSERT INTO "actor" ("first_name", "last_name") VALUES (?, ?)

  def insertActorsStreamList = Action.async {
    insertActorStm
      .streamList(actorList)
      .map(_ => Ok(Json.obj("success" -> true)))
  }

  // insert on conflict

  val managerId = 1.toShort
  val addressId = 3.toShort

  val storeToJson: Tuple4[Int, Short, Short, Instant] => JsValue = {
    case (storeId, managerStaffId, addressId, lastUpdate) =>
      Json.obj(
        "store_id" -> storeId,
        "manager_staff_id" -> managerStaffId,
        "address_id" -> addressId,
        "last_update" -> lastUpdate
      )
  }

  // /insert/store/do-nothing

  /*
    INSERT INTO "store"
    ("manager_staff_id", "address_id")
    VALUES (?, ?)
    ON CONFLICT DO NOTHING
    RETURNING
      "store_id",
      "manager_staff_id",
      "address_id",
      "last_update"
  */

  def insertStoreDoNothing = Action.async {
    db
      .insert(store)
      .cols2(t => (
        t.managerStaffId,
        t.addressId
      ))
      .onConflictDoNothing
      .returning4(_.all)
      .runAs((managerId, addressId))(storeToJson)
      .map {
        case Some(row) => Ok(Json.toJson(row))
        case None => Ok(Json.obj("message" -> "conflict"))
      }
  }

  // /insert/store/do-update

  /*
    INSERT INTO "store" (
      "manager_staff_id",
      "address_id",
      "last_update"
    )
    VALUES (?, ?, ?)
    ON CONFLICT ("manager_staff_id")
    DO UPDATE SET
      "address_id" = ?,
      "last_update" = ?
    RETURNING
      "store_id",
      "manager_staff_id",
      "address_id",
      "last_update"
  */

  val insertStoreDoUpdateStm = db
    .insert(store)
    .cols3(t => (
      t.managerStaffId,
      t.addressId,
      t.lastUpdate
    ))
    .onConflictOnColumn(_.managerStaffId)
    .doUpdate(t => Seq(
      t.addressId,
      t.lastUpdate
    ))
    .returning4(_.all)
    .cache

  def insertStoreDoUpdate = Action.async {
    insertStoreDoUpdateStm
      .runAs((managerId, addressId, Instant.now))(storeToJson)
      .map(row => Ok(Json.toJson(row)))
  }

  // /insert/actors/from-customer

  /*
    INSERT INTO "actor" (
      "first_name",
      "last_name"
    ) SELECT
        "first_name",
        "last_name"
      FROM "customer"
      ORDER BY "customer_id" ASC
      LIMIT ?
  */

  def insertActorsFromCustomer = Action.async {
    db
      .insert(actor)
      .cols2(t => (
        t.firstName,
        t.lastName
      ))
      .fromSelect(
        db
          .select(customer)
          .cols2(t => (
            t.firstName,
            t.lastName
          ))
          .all
          .orderByOne(_.customerId.asc)
          .limit(10)
          .asSubquery
      )
      .runNum()
      .map(num => Ok(Json.obj("rows" -> num)))
  }
  
  // /insert/actors/from-file/:fileName

  /*
    INSERT INTO "actor" (
      "first_name",
      "last_name"
    ) SELECT ?, ?
      WHERE NOT EXISTS (
        SELECT 1
        FROM "actor"
        WHERE "first_name" = ?
        AND "last_name" = ?
      )
  */

  val insertActorsFromFileStm = db
    .insert(actor)
    .cols2(t => (
      t.firstName,
      t.lastName
    ))
    .whereNotExists(t => Seq(
      t.firstName,
      t.lastName
    ))
    .cache

  val splitName: String => Tuple2[String, String] = name => name.span(_ == ' ')

  def insertActorsFromFile = Action.async(parse.json) { request =>

    val fileName = (request.body \ "file_name").as[String]
    val filePath = Paths.get(root, "csv", fileName)

    val source = FileIO
      .fromPath(filePath)
      .via(Framing.delimiter(ByteString("\n"), 256, true))
      .map(_.utf8String)
      .map(splitName)

    insertActorsFromFileStm
      .fromSource(source)
      .map(_ => Ok(Json.obj("success" -> true)))
  }

  // /insert/staff/from-file/:fileName

  /*
    INSERT INTO "staff" (
      "first_name",
      "last_name",
      "address_id",
      "email",
      "active",
      "store_id",
      "username",
      "password"
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  */

  val insertStaffFromFileStm = db
    .insert(staff)
    .cols8(t => (
      t.firstName,
      t.lastName,
      t.addressId,
      t.email,
      t.isActive,
      t.storeId,
      t.username,
      t.password
    ))
    .cache

  val toBool: String => Boolean = {
    case "t" => true
    case _ => false
  }

  val makePwd: String => String = { name =>
    val pwd = name + "12345"
    MessageDigest.getInstance("MD5").digest(pwd.getBytes).map("%02x".format(_)).mkString
  }

  val toStaffRow: String => Tuple8[String, String, Int, String, Boolean, Short, String, String] = { line =>
    line.split(',') match {
      case Array(first, last, email, status) =>
        (first, last, 82, email, toBool(status), 1.toShort, first.toLowerCase, makePwd(first))
      case _ =>
        throw new Exception("invalid file")
    }
  }

  def insertStaffFromFile = Action.async(parse.json) { request =>

    val fileName = (request.body \ "file_name").as[String]
    val filePath = Paths.get(root, "csv", fileName)

    val source = FileIO
      .fromPath(filePath)
      .via(Framing.delimiter(ByteString("\n"), 256, true))
      .map(_.utf8String)
      .map(toStaffRow)

    insertStaffFromFileStm
      .fromSource(source)
      .map(_ => Ok(Json.obj("success" -> true)))
  }
}  




















