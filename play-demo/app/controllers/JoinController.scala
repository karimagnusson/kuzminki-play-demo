package controllers

import java.time._
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import models.dvdrental._
import kuzminki.model._
import models.modeltypes._
import kuzminki.api._
import kuzminki.fn._
import kuzminki.module.KuzminkiPlay


@Singleton
class JoinController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {


  val actor = Model.get[Actor]
  val film = Model.get[Film]
  val filmActor = Model.get[FilmActor]
  val customer = Model.get[Customer]
  val rental = Model.get[Rental]

  //val address = Model.get[Address]
  //val language = Model.get[Language]

  // /actors/film-join/:filmId

  /*
    SELECT
      "a"."actor_id",
      "a"."first_name",
      "a"."last_name"
    FROM "actor" "a"
    INNER JOIN "film_actor" "b"
    ON "a"."actor_id" = "b"."actor_id"
    WHERE "b"."film_id" = ?
    ORDER BY "a"."first_name" ASC
  */

  val filmActorJson: Tuple3[Int, String, String] => JsValue = {
    case (id, first, last) =>
      Json.obj("id" -> id, "name" -> s"$first $last")
  }

  val actorsStm = db
    .select(actor, filmActor)
    .cols3(t => (
      t.a.actorId,
      t.a.firstName,
      t.a.lastName
    ))
    .joinOn(_.actorId, _.actorId)
    .all
    .orderByOne(_.a.firstName.asc)
    .cacheWhere1(_.b.filmId.cacheEq)

  def actorsFilmJoin(filmId: Int) = Action.async {
    actorsStm
      .runAs(filmId)(filmActorJson)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /actors/film-nested/:filmId

  /*
    SELECT
      "actor_id",
      "first_name",
      "last_name",
      "last_update"
    FROM "actor"
    WHERE "actor_id" = ANY(
      SELECT "actor_id"
      FROM "film_actor"
      WHERE "film_id" = ?
    )
    ORDER BY "first_name" ASC
    LIMIT ?
  */

  def actorsFilmNested(filmId: Int) = Action.async {
    db
      .select(actor)
      .colsRead(_.info)
      .whereOne(_.actorId.in(
        db
          .select(filmActor)
          .cols1(_.actorId)
          .whereOne(_.filmId === filmId)
          .asSubquery
      ))
      .orderByOne(_.firstName.asc)
      .limit(10)
      .run()
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /customer/payment/:storeId

  /*
    SELECT
      "a"."customer_id",
      concat_ws(' ', "a"."first_name","a"."last_name"),
      round("b"."amount", ?)::text
    FROM "customer" "a"
    INNER JOIN "payment" "b"
    ON "a"."customer_id" = "b"."customer_id"
    WHERE "a"."activebool" = ?
    AND "a"."store_id" = ?
    ORDER BY "b"."payment_date" DESC
    LIMIT ?
  */
  
  val customerPaymentStm = db
    .select(customerPaymentJoin)
    .colsRead(_.columns)
    .joinOn(_.customerId, _.customerId)
    .whereOne(_.a.isActive === true)
    .orderByOne(_.b.paymentlDate.desc)
    .limit(10)
    .cacheWhere1(_.a.storeId.cacheEq)

  def customerPayment(storeId: Int) = Action.async {
    customerPaymentStm
      .run(storeId.toShort)
      .map(rows => Ok(Json.toJson(rows)))
  }
  
  // /customer/or-and/:storeId

  /*
    SELECT
      "a"."email",
      "b"."rental_date"
    FROM "customer" "a"
    INNER JOIN "rental" "b"
    ON "a"."customer_id" = "b"."customer_id"
    WHERE "a"."activebool" = ?
    AND (
      (
        "b"."rental_date" >= ?
        AND "b"."rental_date" < ?
      )
      OR
      (
        "b"."rental_date" >= ?
        AND "b"."rental_date" < ?
      )
    )
    AND "a"."store_id" = ?
    ORDER BY "b"."rental_date"
    ASC LIMIT ?
  */

  val customerOrAndStm = db
    .select(customer, rental)
    .cols2(t => (
      t.a.email,
      t.b.rentalDate,
    ))
    .joinOn(_.customerId, _.customerId)
    .where(t => Seq(
      t.a.isActive === true,
      Or(
        And(
          t.b.rentalDate >= Instant.parse("2005-05-01T00:00:00Z"),
          t.b.rentalDate < Instant.parse("2005-06-01T00:00:00Z")
        ),
        And(
          t.b.rentalDate >= Instant.parse("2005-07-01T00:00:00Z"),
          t.b.rentalDate < Instant.parse("2005-08-01T00:00:00Z")
        )
      )
    ))
    .orderByOne(_.b.rentalDate.asc)
    .limit(10)
    .cacheWhere1(_.a.storeId.cacheEq)

  val customerOrAndResult: Tuple2[String, Instant] => JsValue = {
    case (email, date) =>
      Json.obj("email" -> email, "date" -> date)
  }

  def customerOrAnd(storeId: Int) = Action.async {
    customerOrAndStm
      .runAs(storeId.toShort)(customerOrAndResult)
      .map(rows => Ok(Json.toJson(rows)))
  }
}











































