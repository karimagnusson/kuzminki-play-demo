package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import kuzminki.api._
import models.world._

// Examples of returning rows as types and using types for insert.

@Singleton
class TypeRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: Kuzminki) extends BaseController {

  val country = Model.get[Country]
  val trip = Model.get[Trip]

  // Writes
  case class CountryType(code: String, name: String, population: Int)
  implicit val countryWrites: Writes[CountryType] = Json.writes[CountryType]

  case class TripType(id: Long, cityId: Int, price: Int)
  implicit val tripWrites: Writes[TripType] = Json.writes[TripType]

  // Reads

  case class TripDataType(cityId: Int, price: Int)
  implicit val tripDataReads: Reads[TripDataType] = Json.reads[TripDataType]

  case class TripPriceType(id: Long, price: Int)
  implicit val tripPriceReads: Reads[TripPriceType] = Json.reads[TripPriceType]

  // Select row as type.

  def selectCountry(code: String) = Action.async {
    sql
      .select(country)
      .cols3(t => (
        t.code,
        t.name,
        t.population
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptType[CountryType]
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }

  // Insert type

  def insertTrip = Action.async(parse.json) { req =>
    sql
      .insert(trip)
      .cols2(t => (
        t.cityId,
        t.price
      ))
      .valuesType(req.body.as[TripDataType])
      .returning3(t => (
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadType[TripType]
      .map(res => Ok(Json.toJson(res)))
  }

  def updateTrip = Action.async(parse.json) { req =>

    val data = req.body.as[TripPriceType]

    sql
      .update(trip)
      .set(_.price ==> data.price)
      .where(_.id === data.id)
      .returning3(t => (
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptType[TripType]
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }
}














