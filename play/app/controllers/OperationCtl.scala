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


@Singleton
class OperationsCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
) (implicit ec: ExecutionContext) extends BaseController
                                     with PlayJson {

  implicit val db = kuzminkiPlay.db

  val trip = Model.get[Trip]

  def insertTrip = Action.async(parse.json) { request =>

    val cityId = (request.body \ "city_id").as[Int]
    val price = (request.body \ "price").as[Int]

    sql
      .insert(trip)
      .cols2(t => (
        t.cityId,
        t.price
      ))
      .values((
        cityId,
        price
      ))
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadAs[JsValue]
      .map(jsonObj(_))
  }

  def updateTrip = Action.async(parse.json) { request =>

    val id = (request.body \ "id").as[Int]
    val price = (request.body \ "price").as[Int]

    sql
      .update(trip)
      .set(_.price ==> price)
      .where(_.id === id)
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  def deleteTrip = Action.async(parse.json) { request =>

    val id = (request.body \ "id").as[Int]

    sql
      .delete(trip)
      .where(_.id === id)
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}




























