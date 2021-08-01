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
import kuzminki.api._
import kuzminki.fn._
import kuzminki.module.KuzminkiPlay


@Singleton
class AggregationController @Inject()(
      val controllerComponents: ControllerComponents,
      val db: KuzminkiPlay
    ) (implicit ec: ExecutionContext) extends BaseController {

  val film = Model.get[Film]
  val customer = Model.get[Customer]
  val rental = Model.get[Rental]
  val payment = Model.get[Payment]

  // /film/length

  /*
    SELECT
      round(avg("length"), ?)::text,
      max("length"),
      min("length")
    FROM "film"
    WHERE
      "length" IS NOT NULL
  */

  def filmLength = Action.async {
    db
      .select(film)
      .cols3(t => (
        Avg.short(t.length).round(2).asString,
        Max.short(t.length),
        Min.short(t.length)
      ))
      .whereOne(_.length.isNotNull)
      .head()
      .map {
        case (avg, max, min) =>
          Ok(
            Json.obj("avg" -> avg, "max" -> max, "min" -> min)
          )
      }
  }

  // /rental/data

  /*
    SELECT
      count(*),
      max("b"."rental_date"),
      min("b"."rental_date"),
      "a"."customer_id"
    FROM "customer" "a"
    INNER JOIN "rental" "b"
    ON "a"."customer_id" = "b"."customer_id"
    GROUP BY "a"."customer_id"
    HAVING
      "a"."activebool" = ?
      AND count(*) > ?
    ORDER BY
      count(*) DESC,
      "a"."customer_id" ASC
    LIMIT ?
  */

  val rentalDataStm = db
    .select(customer, rental)
    .cols4(t => (
      Count.all,
      Max.instant(t.b.rentalDate),
      Min.instant(t.b.rentalDate),
      t.a.customerId,
    ))
    .joinOn(_.customerId, _.customerId)
    .groupByOne(_.a.customerId)
    .having(t => Seq(
      t.a.isActive === true,
      Count.all > 1
    ))
    .orderBy(t => Seq(
      Count.all.desc,
      t.a.customerId.asc
    ))
    .limit(10)
    .cache

  val rentalDataResult: Tuple4[Long, Instant, Instant, Int] => JsValue = {
    case (count, maxDate, minDate, id) =>
      Json.obj(
        "count" -> count,
        "last_rental" -> maxDate,
        "first_rental" -> minDate,
        "customer_id" -> id
      )
  }

  def rentalData = Action.async {
    rentalDataStm
      .runAs(rentalDataResult)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /customer/spending/:storeId

  /*
    SELECT
      count(*),
      round(sum("b"."amount"), ?)::text,
      round(avg("b"."amount"), ?)::text,
      max("b"."amount")::text,
      min("b"."amount")::text,
      "a"."customer_id"
    FROM "customer" "a"
    INNER JOIN "payment" "b"
    ON "a"."customer_id" = "b"."customer_id"
    GROUP BY "a"."customer_id"
    HAVING "a"."store_id" = ?
    ORDER BY sum("b"."amount")
    DESC LIMIT ?
  */

  val customerSpendingStm = db
    .select(customer, payment)
    .cols6(t => (
      Count.all,
      Sum.numeric(t.b.amount).round(2).asString,
      Avg.numeric(t.b.amount).round(2).asString,
      Max.numeric(t.b.amount).asString,
      Min.numeric(t.b.amount).asString,
      t.a.customerId,
    ))
    .joinOn(_.customerId, _.customerId)
    .groupByOne(_.a.customerId)
    .all
    .orderByOne(t => Sum.numeric(t.b.amount).desc)
    .limit(10)
    .cacheHaving1(_.a.storeId.cacheEq)

  val customerSpendingResult: Tuple6[Long, String, String, String, String, Int] => JsValue = {
    case (count, sum, avg, max, min, id) =>
      Json.obj(
        "count" -> count,
        "sum" -> sum,
        "avg" -> avg,
        "max" -> max,
        "min" -> min,
        "customer_id" -> id,
      )
  }

  def customerSpending(storeId: Int) = Action.async {
    customerSpendingStm
      .runAs(storeId.toShort)(customerSpendingResult)
      .map(rows => Ok(Json.toJson(rows)))
  }

  // /customer/low-spending/:storeId

  /*
    SELECT
      round(avg("b"."amount"), ?)::text,
      "a"."customer_id",
      "a"."first_name",
      "a"."last_name"
    FROM "customer" "a"
    INNER JOIN "payment" "b"
    ON "a"."customer_id" = "b"."customer_id"
    GROUP BY "a"."customer_id"
    HAVING avg("b"."amount") < (
      SELECT avg("amount")
      FROM "payment"
    )
    AND "a"."store_id" = ?
    ORDER BY sum("b"."amount")
    DESC LIMIT ?
  */

  val customerLowSpendingStm = db
    .select(customer, payment)
    .cols4(t => (
      Avg.numeric(t.b.amount).round(2).asString,
      t.a.customerId,
      t.a.firstName,
      t.a.lastName
    ))
    .joinOn(_.customerId, _.customerId)
    .groupByOne(_.a.customerId)
    .havingOne(t => Avg.numeric(t.b.amount).lt(
      db
        .select(payment)
        .cols1(t => Avg.numeric(t.amount))
        .all
        .asAggregation
    ))
    .orderByOne(t => Sum.numeric(t.b.amount).desc)
    .limit(10)
    .cacheHaving1(_.a.storeId.cacheEq)

  val customerLowSpendingResult: Tuple4[String, Int, String, String] => JsValue = {
    case (avg, id, first, last) =>
      Json.obj(
        "avg" -> avg,
        "customer_id" -> id,
        "first_name" -> first,
        "last_name" -> last
      )
  }

  def customerLowSpending(storeId: Int) = Action.async {
    customerLowSpendingStm
      .runAs(storeId.toShort)(customerLowSpendingResult)
      .map(rows => Ok(Json.toJson(rows)))
  }
}


















