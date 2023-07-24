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
class SelectTypeCtl @Inject()(
  val controllerComponents: ControllerComponents,
  val kuzminkiPlay: KuzminkiPlay
)(implicit ec: ExecutionContext) extends BaseController {

  implicit val db = kuzminkiPlay.db

  val country = Model.get[Country]

  implicit val countryBasicWrites = Json.writes[CountryBasic]

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














