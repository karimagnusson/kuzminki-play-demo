package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import kuzminki.api._
import kuzminki.pekko.play.json.PlayJson
import demo.responses.PlayJsonDemo
import models.world._

// INSERT, UPDATE, DELETE

@Singleton
class OperationsRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: Kuzminki) extends BaseController
                            with PlayJson
                            with PlayJsonDemo {

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




























