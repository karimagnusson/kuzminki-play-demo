package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import kuzminki.api._
import models.world._


@Singleton
class SelectTypeRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: Kuzminki) extends BaseController {

  val country = Model.get[Country]

  implicit val countryBasicWrites: Writes[CountryBasic] = Json.writes[CountryBasic]

  def selectCountry(code: String) = Action.async {
    sql
      .select(country)
      .colsType(_.basic) // "basic" is defined in the table
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }
}














