package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import utils._
import models.world._
import modules.KuzminkiPlay
import kuzminki.api._
import kuzminki.fn._

// Examples of working with timestamp field.

@Singleton
class DateCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
)(implicit ec: ExecutionContext) extends BaseController
                                    with PlayJson {

  implicit val db = kuzminkiPlay.db

  val btcPrice = Model.get[BtcPrice]

  def btcHour = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0)).toMap
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        t.high.round(2),
        t.low.round(2),
        t.open.round(2),
        t.close.round(2),
        t.stime.format("DD Mon YYYY HH24:MI")
      ))
      .where(t => Seq(
        t.stime.year === params("year").toInt, // pick year from timestamp
        t.stime.doy === params("doy").toInt    // pick day of year from timestamp
      ))
      .orderBy(_.stime.asc)
      .runAs[JsValue]
      .map(jsonList(_))
  }

  def btcQuarterAvg = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0)).toMap
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        "avg" -> Agg.avg(t.close).round(2),
        "max" -> Agg.max(t.close).round(2),
        "min" -> Agg.min(t.close).round(2)
      ))
      .where(t => Seq(
        t.stime.year === params("year").toInt,
        t.stime.quarter === params("quarter").toInt
      ))
      .runHeadAs[JsValue]
      .map(jsonObj(_))
  }

  def btcBreak = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0)).toMap
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        "price" -> t.high.round(2),
        "year" -> t.stime.year,
        "quarter" -> t.stime.quarter,
        "week" -> t.stime.week,
        "date" -> t.stime.format("DD Mon YYYY HH24:MI")
      ))
      .where(_.high >= BigDecimal(params("price")))
      .orderBy(_.high.asc)
      .limit(1)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}













