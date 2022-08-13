package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import models.worlddb._
import models.PlayJsonLoader
import kuzminki.module.KuzminkiPlay
import kuzminki.api._


@Singleton
class TripController @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
) (implicit ec: ExecutionContext) extends BaseController {

  implicit val db = kuzminkiPlay.db
  implicit val loadJson: Seq[Tuple2[String, Any]] => JsValue = { data =>
    PlayJsonLoader.load(data)
  }

  val trip = Model.get[Trip]
  val city = Model.get[City]
  val country = Model.get[Country]

  val notFound = Json.obj("message" -> "not found")

  def tripList = Action.async {
    sql
      .select(trip, city)
      .colsNamed(t => Seq(
        t.a.id,
        t.a.price,
        t.b.name,
        t.b.countryCode
      ))
      .joinOn(_.cityId, _.id)
      .all
      .runAs[JsValue]
      .map(JsArray(_))
      .map(Ok(_))
  }

  def addTrip = Action.async(parse.json) { request =>

    val cityId = (request.body \ "city_id").as[Int]
    val price = (request.body \ "price").as[Int]

    sql
      .insert(trip)
      .cols2(t => (
        t.cityId,
        t.price
      ))
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadAs[JsValue]((cityId, price))
      .map(Ok(_))
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
      .map {
        case Some(data) => Ok(data)
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }

  def deleteTrip = Action.async(parse.json) { request =>

    val id = (request.body \ "id").as[Int]

    sql
      .delete(trip)
      .where(_.id === id)
      .runNum
      .map(num => Ok(Json.obj("deleted" -> num)))
  }
}




























